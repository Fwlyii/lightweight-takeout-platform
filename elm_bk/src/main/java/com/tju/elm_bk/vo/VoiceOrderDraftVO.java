package com.tju.elm_bk.vo;

import java.math.BigDecimal;
import java.util.List;

public record VoiceOrderDraftVO(
        String transcript,
        String query,
        int quantity,
        String specification,
        BigDecimal budget,
        List<AiRecommendationVO> candidates,
        String provider
) {
}
