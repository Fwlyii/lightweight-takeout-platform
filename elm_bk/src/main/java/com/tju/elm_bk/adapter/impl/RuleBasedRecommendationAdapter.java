package com.tju.elm_bk.adapter.impl;

import com.tju.elm_bk.adapter.RecommendationAdapter;
import com.tju.elm_bk.entity.UserPreference;
import com.tju.elm_bk.mapper.FoodMapper;
import com.tju.elm_bk.vo.AiFoodCandidateVO;
import com.tju.elm_bk.vo.AiRecommendationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RuleBasedRecommendationAdapter implements RecommendationAdapter {
    private static final int CANDIDATE_LIMIT = 200;
    private static final Set<String> SPICY_WORDS = Set.of("辣", "麻辣", "香辣", "川味", "剁椒", "藤椒", "泡椒");
    private static final String QUERY_NOISE =
            "(?:麻烦|请|可以|能不能|帮我|给我|我要|我想要|我想吃|想吃|推荐|来点|来一份|点一份|一些|有没有|适合|左右|在售|商品|菜品|美食|热门|热销)";

    private final FoodMapper foodMapper;

    @Override
    public List<AiRecommendationVO> recommend(RecommendationContext context) {
        List<AiFoodCandidateVO> candidates = foodMapper.listAiCandidates(context.userId(), CANDIDATE_LIMIT);
        List<String> queryTerms = terms(context.query());
        List<String> tasteTags = tags(context.preference() == null ? null : context.preference().getTasteTags());
        List<String> categoryTags = tags(context.preference() == null ? null : context.preference().getCategoryTags());
        List<String> avoidTags = tags(context.preference() == null ? null : context.preference().getAvoidTags());

        return candidates.stream()
                .filter(candidate -> available(candidate, context.quantity()))
                .filter(candidate -> matchesFilters(candidate, context))
                .filter(candidate -> avoidTags.stream().noneMatch(tag -> searchable(candidate).contains(normalize(tag))))
                .map(candidate -> score(candidate, context, queryTerms, tasteTags, categoryTags))
                .filter(scored -> queryTerms.isEmpty() || scored.queryMatches() > 0)
                .sorted(Comparator.comparingDouble(ScoredCandidate::score).reversed()
                        .thenComparing(ScoredCandidate::salesCount, Comparator.reverseOrder())
                        .thenComparing(ScoredCandidate::userPurchaseCount, Comparator.reverseOrder())
                        .thenComparing(scored -> scored.candidate().getFoodId()))
                .limit(Math.max(1, Math.min(context.limit(), 20)))
                .map(this::toRecommendation)
                .toList();
    }

    private ScoredCandidate score(AiFoodCandidateVO candidate,
                                  RecommendationContext context,
                                  List<String> queryTerms,
                                  List<String> tasteTags,
                                  List<String> categoryTags) {
        String text = searchable(candidate);
        int queryMatches = matches(text, queryTerms);
        int tasteMatches = matches(text, tasteTags);
        int categoryMatches = matches(text, categoryTags);
        long sales = safe(candidate.getSalesCount());
        long purchases = safe(candidate.getUserPurchaseCount());
        double score = queryMatches * 18.0
                + tasteMatches * 7.0
                + categoryMatches * 8.0
                + Math.min(purchases, 10) * 5.0
                + Math.log1p(sales) * 3.0;

        UserPreference preference = context.preference();
        if (preference != null && preference.getSpicyLevel() != null) {
            boolean spicy = SPICY_WORDS.stream().anyMatch(text::contains);
            if (spicy && preference.getSpicyLevel() <= 0) score -= 12;
            if (spicy && preference.getSpicyLevel() >= 2) score += 8;
            if (!spicy && preference.getSpicyLevel() <= 0) score += 3;
        }
        Pricing pricing = pricing(candidate, context.quantity());
        return new ScoredCandidate(candidate, score, queryMatches, tasteMatches + categoryMatches,
                sales, purchases, context.budget() != null, pricing, context.quantity());
    }

    private AiRecommendationVO toRecommendation(ScoredCandidate scored) {
        AiFoodCandidateVO candidate = scored.candidate();
        List<String> reasons = new ArrayList<>();
        if (scored.queryMatches() > 0) reasons.add("符合你的文字需求");
        if (scored.userPurchaseCount() > 0) reasons.add("你曾购买过");
        if (scored.preferenceMatches() > 0) reasons.add("符合已授权偏好");
        if (scored.salesCount() > 0) reasons.add("平台已完成订单中销量较高");
        if (scored.withinBudget()) reasons.add("最低可结算总额不超过预算");
        if (reasons.isEmpty()) reasons.add("当前可售热销候选");
        AiRecommendationVO result = new AiRecommendationVO(candidate.getFoodId(), candidate.getFoodName(), candidate.getPrice(),
                candidate.getFoodImg(), candidate.getBusinessId(), candidate.getBusinessName(),
                String.join("，", reasons));
        result.setQuantity(scored.quantity());
        result.setSubtotal(scored.pricing().subtotal());
        result.setDeliveryPrice(scored.pricing().deliveryPrice());
        result.setStartPrice(scored.pricing().startPrice());
        result.setPromotionThreshold(candidate.getPromotionThreshold());
        result.setPromotionDiscount(scored.pricing().promotionDiscount());
        result.setEstimatedTotal(scored.pricing().minimumCheckoutTotal());
        result.setAmountToStartPrice(scored.pricing().amountToStartPrice());
        result.setBusinessScore(candidate.getBusinessScore());
        result.setBusinessSalesCount(candidate.getBusinessSalesCount());
        result.setStock(candidate.getStock());
        result.setPurchaseLimit(candidate.getPurchaseLimit());
        result.setCategory(candidate.getCategory());
        result.setOperatingStatus(candidate.getOperatingStatus());
        return result;
    }

    private boolean available(AiFoodCandidateVO candidate, int quantity) {
        if (Boolean.FALSE.equals(candidate.getOperatingStatus())) return false;
        if (candidate.getPrice() == null || candidate.getStock() == null || candidate.getStock() < quantity) return false;
        return candidate.getPurchaseLimit() == null || candidate.getPurchaseLimit() <= 0
                || quantity <= candidate.getPurchaseLimit();
    }

    private boolean matchesFilters(AiFoodCandidateVO candidate, RecommendationContext context) {
        BigDecimal delivery = money(candidate.getDeliveryPrice());
        if (context.freeDeliveryOnly() && delivery.compareTo(BigDecimal.ZERO) > 0) return false;
        if (context.maxDeliveryFee() != null && delivery.compareTo(context.maxDeliveryFee()) > 0) return false;
        if (context.minRating() != null && money(candidate.getBusinessScore()).compareTo(context.minRating()) < 0) return false;
        if (context.category() != null && !context.category().isBlank()
                && !normalize(candidate.getCategory()).contains(normalize(context.category()))) return false;
        Pricing pricing = pricing(candidate, context.quantity());
        return context.budget() == null || pricing.minimumCheckoutTotal().compareTo(context.budget()) <= 0;
    }

    private Pricing pricing(AiFoodCandidateVO candidate, int quantity) {
        BigDecimal subtotal = candidate.getPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal delivery = money(candidate.getDeliveryPrice());
        BigDecimal start = money(candidate.getStartPrice());
        BigDecimal checkoutSubtotal = subtotal.max(start);
        BigDecimal threshold = candidate.getPromotionThreshold();
        BigDecimal configuredDiscount = money(candidate.getPromotionDiscount());
        BigDecimal discount = threshold != null && threshold.compareTo(BigDecimal.ZERO) > 0
                && checkoutSubtotal.compareTo(threshold) >= 0 ? configuredDiscount : BigDecimal.ZERO;
        BigDecimal total = checkoutSubtotal.add(delivery).subtract(discount).max(BigDecimal.ZERO);
        return new Pricing(subtotal, delivery, start, discount, start.subtract(subtotal).max(BigDecimal.ZERO), total);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int matches(String text, List<String> values) {
        return (int) values.stream().map(this::normalize).filter(value -> !value.isBlank())
                .filter(text::contains).distinct().count();
    }

    private List<String> terms(String query) {
        String cleaned = normalize(query).replaceAll(QUERY_NOISE, " ")
                .replaceAll("(?:预算|不超过|最多|控制在|价格在?)?\\s*\\d+(?:\\.\\d{1,2})?\\s*(?:元|块)(?:以内|以下|左右)?", " ")
                .replaceAll("[，。！？、,.!?;；:/\\s]+", " ").trim();
        if (cleaned.isEmpty()) return List.of();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String part : cleaned.split("\\s+")) {
            if (!part.isBlank()) result.add(part);
            if (part.matches(".*[\\u4e00-\\u9fa5].*") && part.length() > 2) {
                for (int i = 0; i < part.length() - 1; i++) result.add(part.substring(i, i + 2));
            }
        }
        return new ArrayList<>(result);
    }

    private List<String> tags(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return List.of(raw.split("[,，、\\s]+"));
    }

    private String searchable(AiFoodCandidateVO candidate) {
        return normalize(String.join(" ", safe(candidate.getFoodName()), safe(candidate.getFoodExplain()),
                safe(candidate.getCategory()), safe(candidate.getBusinessName())));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private long safe(Long value) {
        return value == null ? 0 : value;
    }

    private record ScoredCandidate(AiFoodCandidateVO candidate, double score, int queryMatches,
                                   int preferenceMatches, Long salesCount, Long userPurchaseCount,
                                   boolean withinBudget, Pricing pricing, int quantity) {
    }

    private record Pricing(BigDecimal subtotal, BigDecimal deliveryPrice, BigDecimal startPrice,
                           BigDecimal promotionDiscount, BigDecimal amountToStartPrice,
                           BigDecimal minimumCheckoutTotal) {
    }
}
