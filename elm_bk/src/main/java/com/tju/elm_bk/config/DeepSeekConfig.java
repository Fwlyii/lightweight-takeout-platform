package com.tju.elm_bk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekConfig {
    
    private String apiKey = "";
    
    private String baseUrl = "https://api.deepseek.com";
    
    private String chatEndpoint = "/chat/completions";
    
    private String model = "deepseek-v4-flash";

    private Boolean thinkingEnabled = false;
    
    private Integer maxTokens = 1024;
    
    private Double temperature = 0.7;
    
    private Double topP = 0.9;
    
    private Integer timeoutSeconds = 10;
    
    private Integer maxRetries = 1;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
