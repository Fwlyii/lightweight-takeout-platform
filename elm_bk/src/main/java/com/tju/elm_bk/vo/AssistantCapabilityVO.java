package com.tju.elm_bk.vo;

public record AssistantCapabilityVO(
        boolean textChat,
        boolean imageRecognition,
        boolean speechRecognition,
        String imageProvider,
        String speechProvider
) {
}
