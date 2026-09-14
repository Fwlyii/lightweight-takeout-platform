package com.tju.elm_bk.adapter.impl;

import com.tju.elm_bk.adapter.SpeechAdapter;
import com.tju.elm_bk.client.DashScopeMultimodalClient;
import com.tju.elm_bk.config.MultimodalAiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@Profile("!ai-mock")
@RequiredArgsConstructor
public class DashScopeSpeechAdapter implements SpeechAdapter {
    private final DashScopeMultimodalClient client;
    private final MultimodalAiConfig config;

    @Override
    public SpeechTranscriptionResult transcribe(byte[] content, String mimeType) {
        String dataUri = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(content);
        Map<String, Object> request = Map.of(
                "model", config.getSpeechModel(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(Map.of(
                                "type", "input_audio",
                                "input_audio", Map.of("data", dataUri))))),
                "asr_options", Map.of("language", "zh", "enable_itn", true),
                "stream", false
        );
        String transcript = client.extractText(client.chatCompletion(request)).trim();
        return new SpeechTranscriptionResult(transcript, "zh-CN");
    }

    @Override
    public boolean isAvailable() {
        return config.isConfigured();
    }

    @Override
    public String provider() {
        return "DashScope/" + config.getSpeechModel();
    }
}
