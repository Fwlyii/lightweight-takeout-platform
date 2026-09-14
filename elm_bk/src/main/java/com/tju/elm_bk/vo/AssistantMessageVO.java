package com.tju.elm_bk.vo;

import java.util.List;

public record AssistantMessageVO(
        String intent,
        String message,
        String sessionId,
        List<AiRecommendationVO> candidates,
        boolean needConfirmation,
        long processingTime
) {
}
