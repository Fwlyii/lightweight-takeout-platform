package com.tju.elm_bk.adapter;

import com.tju.elm_bk.entity.UserPreference;
import com.tju.elm_bk.vo.AiRecommendationVO;

import java.math.BigDecimal;
import java.util.List;

public interface RecommendationAdapter {
    List<AiRecommendationVO> recommend(RecommendationContext context);

    record RecommendationContext(
            String query,
            BigDecimal budget,
            Long userId,
            UserPreference preference,
            int limit,
            int quantity,
            BigDecimal maxDeliveryFee,
            BigDecimal minRating,
            boolean freeDeliveryOnly,
            String category
    ) {
        public RecommendationContext(String query, BigDecimal budget, Long userId,
                                     UserPreference preference, int limit) {
            this(query, budget, userId, preference, limit, 1, null, null, false, null);
        }
    }
}
