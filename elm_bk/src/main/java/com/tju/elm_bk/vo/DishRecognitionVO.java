package com.tju.elm_bk.vo;

import java.util.List;

public record DishRecognitionVO(
        String summary,
        List<String> keywords,
        double confidence,
        List<AiRecommendationVO> candidates,
        String provider
) {
}
