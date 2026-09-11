package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.adapter.RecommendationAdapter;
import com.tju.elm_bk.dto.RecommendationRequestDTO;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.entity.UserPreference;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.PreferenceMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.service.AiRecommendationService;
import com.tju.elm_bk.utils.SecurityUtils;
import com.tju.elm_bk.vo.AiRecommendationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiRecommendationServiceImpl implements AiRecommendationService {
    private static final Pattern PREFIX_BUDGET = Pattern.compile("(?:预算|不超过|最多|控制在|价格在?)\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块)?");
    private static final Pattern SUFFIX_BUDGET = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块)(?:以内|以下)");
    private final RecommendationAdapter recommendationAdapter;
    private final PreferenceMapper preferenceMapper;
    private final UserMapper userMapper;

    @Override
    public List<AiRecommendationVO> recommend(String query, BigDecimal budget, boolean usePreferences) {
        RecommendationRequestDTO request = new RecommendationRequestDTO();
        request.setQuery(query);
        request.setBudget(budget);
        request.setUsePreferences(usePreferences);
        return recommend(request);
    }

    @Override
    public List<AiRecommendationVO> recommend(RecommendationRequestDTO request) {
        RecommendationRequestDTO safeRequest = request == null ? new RecommendationRequestDTO() : request;
        String query = safeRequest.getQuery();
        BigDecimal budget = safeRequest.getBudget();
        if (budget == null) budget = parseBudget(query);
        if (budget != null && (budget.compareTo(BigDecimal.ZERO) <= 0 || budget.compareTo(new BigDecimal("9999.99")) > 0)) {
            throw new APIException("预算必须在0.01到9999.99之间");
        }
        int quantity = safeRequest.getQuantity() == null ? 1 : safeRequest.getQuantity();
        if (quantity < 1 || quantity > 99) throw new APIException("数量必须在1到99之间");
        User user = Boolean.TRUE.equals(safeRequest.getUsePreferences()) ? currentUser() : null;
        UserPreference preference = user == null ? null : preferenceMapper.findByUserId(user.getId());
        return recommendationAdapter.recommend(new RecommendationAdapter.RecommendationContext(
                query == null ? "" : query.trim(), budget, user == null ? null : user.getId(), preference, 6,
                quantity, safeRequest.getMaxDeliveryFee(), safeRequest.getMinRating(),
                Boolean.TRUE.equals(safeRequest.getFreeDeliveryOnly()), safeRequest.getCategory()));
    }

    private BigDecimal parseBudget(String query) {
        if (query == null) return null;
        for (Pattern pattern : List.of(PREFIX_BUDGET, SUFFIX_BUDGET)) {
            Matcher matcher = pattern.matcher(query);
            if (matcher.find()) {
                BigDecimal parsed = new BigDecimal(matcher.group(1));
                if (parsed.compareTo(BigDecimal.ZERO) > 0 && parsed.compareTo(new BigDecimal("9999.99")) <= 0) {
                    return parsed;
                }
            }
        }
        return null;
    }

    private User currentUser() {
        try {
            return userMapper.findByUsername(SecurityUtils.getCurrentUsername().orElse(""));
        } catch (Exception ignored) {
            return null;
        }
    }
}
