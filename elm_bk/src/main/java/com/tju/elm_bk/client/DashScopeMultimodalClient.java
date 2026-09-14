package com.tju.elm_bk.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.tju.elm_bk.config.MultimodalAiConfig;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.utils.AiCircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class DashScopeMultimodalClient {
    private final MultimodalAiConfig config;
    private final WebClient webClient;
    private final AiCircuitBreaker circuitBreaker;

    public DashScopeMultimodalClient(MultimodalAiConfig config, AiCircuitBreaker circuitBreaker) {
        this.config = config;
        this.circuitBreaker = circuitBreaker;
        this.webClient = WebClient.builder()
                .baseUrl(removeTrailingSlash(config.getBaseUrl()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public JsonNode chatCompletion(Map<String, Object> request) {
        if (!config.isConfigured()) {
            throw new APIException("多模态AI未配置，请设置DASHSCOPE_API_KEY和DASHSCOPE_BASE_URL");
        }
        try {
            return circuitBreaker.execute("dashscope", () -> doChatCompletion(request));
        } catch (APIException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("DashScope请求失败: {}", conciseMessage(ex));
            throw new APIException("智能识别服务暂时不可用，请稍后重试");
        }
    }

    private JsonNode doChatCompletion(Map<String, Object> request) {
        JsonNode response = webClient.post()
                .uri(config.getChatEndpoint())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .retryWhen(Retry.backoff(config.getMaxRetries(), Duration.ofMillis(400))
                        .filter(this::shouldRetry))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();
        if (response == null) throw new APIException("多模态AI返回为空");
        return response;
    }

    public String extractText(JsonNode response) {
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        if (content.isTextual()) return content.asText().trim();
        if (content.isArray()) {
            StringBuilder text = new StringBuilder();
            content.forEach(item -> {
                String part = item.path("text").asText("").trim();
                if (!part.isEmpty()) {
                    if (!text.isEmpty()) text.append('\n');
                    text.append(part);
                }
            });
            return text.toString();
        }
        throw new APIException("无法解析智能识别结果");
    }

    private boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            int status = ex.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return true;
    }

    private static String removeTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "http://localhost";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String conciseMessage(Exception ex) {
        if (ex instanceof WebClientResponseException responseException) {
            return String.valueOf(responseException.getStatusCode().value());
        }
        return ex.getClass().getSimpleName();
    }
}
