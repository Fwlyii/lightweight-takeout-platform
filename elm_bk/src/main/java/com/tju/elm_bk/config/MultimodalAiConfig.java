package com.tju.elm_bk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "multimodal-ai")
public class MultimodalAiConfig {
    private String apiKey = "";
    private String baseUrl = "";
    private String chatEndpoint = "/chat/completions";
    private String visionModel = "qwen-vl-plus";
    private String speechModel = "qwen3-asr-flash";
    private int timeoutSeconds = 10;
    private int maxRetries = 1;
    private long maxImageBytes = 5L * 1024 * 1024;
    private long maxAudioBytes = 7L * 1024 * 1024;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && baseUrl != null && !baseUrl.isBlank();
    }
}
