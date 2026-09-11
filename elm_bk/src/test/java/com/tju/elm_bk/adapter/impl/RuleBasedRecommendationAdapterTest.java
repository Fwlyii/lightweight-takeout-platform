package com.tju.elm_bk.adapter.impl;

import com.tju.elm_bk.adapter.RecommendationAdapter;
import com.tju.elm_bk.entity.UserPreference;
import com.tju.elm_bk.mapper.FoodMapper;
import com.tju.elm_bk.vo.AiFoodCandidateVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleBasedRecommendationAdapterTest {
    private final FoodMapper foodMapper = mock(FoodMapper.class);
    private final RuleBasedRecommendationAdapter adapter = new RuleBasedRecommendationAdapter(foodMapper);

    @Test
    void shouldUseCompletedSalesForColdStart() {
        when(foodMapper.listAiCandidates(null, 200)).thenReturn(List.of(
                food(1L, "普通面", "面食", 3L, 0L, "18"),
                food(2L, "热销饭", "盖饭", 30L, 0L, "22")));

        var result = adapter.recommend(new RecommendationAdapter.RecommendationContext("", null, null, null, 6));

        assertEquals(2L, result.get(0).getFoodId());
        assertTrue(result.get(0).getReason().contains("销量"));
    }

    @Test
    void shouldApplyQueryBudgetAvoidTagsAndPurchaseHistory() {
        UserPreference preference = new UserPreference();
        preference.setAvoidTags("香菜");
        preference.setTasteTags("清淡");
        preference.setCategoryTags("面食");
        preference.setSpicyLevel(0);
        when(foodMapper.listAiCandidates(7L, 200)).thenReturn(List.of(
                food(1L, "清汤牛肉面", "清淡面食", 5L, 4L, "26"),
                food(2L, "香菜牛肉面", "清淡面食", 50L, 0L, "20"),
                food(3L, "麻辣牛肉面", "麻辣面食", 80L, 0L, "35")));

        var result = adapter.recommend(new RecommendationAdapter.RecommendationContext(
                "想吃清淡牛肉面", new BigDecimal("30"), 7L, preference, 6));

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getFoodId());
        assertTrue(result.get(0).getReason().contains("曾购买"));
        assertTrue(result.get(0).getReason().contains("不超过预算"));
    }

    @Test
    void shouldRejectQuantityWhenCheckoutTotalExceedsBudget() {
        when(foodMapper.listAiCandidates(null, 200)).thenReturn(List.of(
                food(1L, "牛肉面", "招牌牛肉面", 10L, 0L, "18")));

        var result = adapter.recommend(new RecommendationAdapter.RecommendationContext(
                "牛肉面", new BigDecimal("30"), null, null, 6, 2, null, null, false, null));

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldCalculateStartPriceDeliveryAndPromotion() {
        AiFoodCandidateVO candidate = food(1L, "牛肉饭", "招牌盖饭", 10L, 0L, "20");
        candidate.setStartPrice(new BigDecimal("30"));
        candidate.setDeliveryPrice(new BigDecimal("5"));
        candidate.setPromotionThreshold(new BigDecimal("30"));
        candidate.setPromotionDiscount(new BigDecimal("8"));
        when(foodMapper.listAiCandidates(null, 200)).thenReturn(List.of(candidate));

        var result = adapter.recommend(new RecommendationAdapter.RecommendationContext(
                "牛肉饭", new BigDecimal("27"), null, null, 6, 1, null, null, false, null));

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("20"), result.get(0).getSubtotal());
        assertEquals(new BigDecimal("10"), result.get(0).getAmountToStartPrice());
        assertEquals(new BigDecimal("27"), result.get(0).getEstimatedTotal());
    }

    private AiFoodCandidateVO food(Long id, String name, String explain, Long sales, Long purchases, String price) {
        AiFoodCandidateVO candidate = new AiFoodCandidateVO();
        candidate.setFoodId(id);
        candidate.setFoodName(name);
        candidate.setFoodExplain(explain);
        candidate.setCategory("面食");
        candidate.setPrice(new BigDecimal(price));
        candidate.setBusinessId(10L);
        candidate.setBusinessName("测试商家");
        candidate.setSalesCount(sales);
        candidate.setUserPurchaseCount(purchases);
        candidate.setFoodImg("/images/foods/04-noodles.jpg");
        candidate.setDeliveryPrice(BigDecimal.ZERO);
        candidate.setStartPrice(BigDecimal.ZERO);
        candidate.setBusinessScore(new BigDecimal("4.8"));
        candidate.setBusinessSalesCount(1200);
        candidate.setStock(99);
        candidate.setOperatingStatus(true);
        return candidate;
    }
}
