package com.tju.elm_bk.service;

import com.tju.elm_bk.vo.BusinessSearchVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessRecommendationPolicyTest {

    private final BusinessPricingPolicy pricingPolicy = new BusinessPricingPolicy();
    private final BusinessRecommendationPolicy policy = new BusinessRecommendationPolicy(pricingPolicy);

    @Test
    void buildsAtMostThreeTagsWithStablePriority() {
        BusinessSearchVO business = business("4.80", 600);
        business.setPromotionThreshold(new BigDecimal("20"));
        business.setPromotionDiscount(new BigDecimal("3"));
        business.setDineInAvailable(true);

        policy.enrich(business, true);

        assertEquals(3, business.getRecommendationTags().size());
        assertEquals("\u4e0a\u6b21\u4e70\u8fc7", business.getRecommendationTags().get(0));
        assertEquals("\u6ee120\u51cf3", business.getRecommendationTags().get(1));
        assertTrue(business.getRecommendationTags().get(2).contains("\u7231\u4e0d\u91ca\u624b"));
    }

    @Test
    void reputationLabelsRequireEnoughCompletedSales() {
        BusinessSearchVO business = business("4.90", 20);

        policy.enrich(business, false);

        assertFalse(business.getRecommendationTags().stream()
                .anyMatch(tag -> tag.contains("\u597d\u8bc4") || tag.contains("\u7231\u4e0d\u91ca\u624b")));
    }

    @Test
    void recommendationReasonComesFromRealFields() {
        BusinessSearchVO popular = business("4.80", 900);
        policy.enrich(popular, false);
        assertEquals("\u4eba\u6c14\u63a8\u8350", popular.getRecommendationReason());

        BusinessSearchVO breakfast = business("4.20", 60);
        breakfast.setOrderTypeId(2);
        policy.enrich(breakfast, false);
        assertEquals("\u65e9\u9910\u4f18\u9009", breakfast.getRecommendationReason());

        BusinessSearchVO quiet = business("4.10", 30);
        quiet.setOrderTypeId(10);
        policy.enrich(quiet, false);
        assertEquals(null, quiet.getRecommendationReason());
    }

    private BusinessSearchVO business(String score, int sales) {
        BusinessSearchVO business = new BusinessSearchVO();
        business.setId(1L);
        business.setScore(new BigDecimal(score));
        business.setSalesCount(sales);
        business.setStartPrice(new BigDecimal("25"));
        business.setDeliveryPrice(new BigDecimal("3"));
        business.setCreateTime(LocalDateTime.now().minusDays(90));
        business.setOperatingStatus(true);
        return business;
    }
}
