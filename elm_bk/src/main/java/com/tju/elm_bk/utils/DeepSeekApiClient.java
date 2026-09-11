package com.tju.elm_bk.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tju.elm_bk.config.DeepSeekConfig;
import com.tju.elm_bk.dto.DeepSeekRequestDTO;
import com.tju.elm_bk.dto.DeepSeekResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
public class DeepSeekApiClient {
    private final DeepSeekConfig deepSeekConfig;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final AiCircuitBreaker circuitBreaker;

    public DeepSeekApiClient(DeepSeekConfig deepSeekConfig, ObjectMapper objectMapper,
                             AiCircuitBreaker circuitBreaker) {
        this.deepSeekConfig = deepSeekConfig;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreaker;
        this.webClient = WebClient.builder()
                .baseUrl(deepSeekConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + deepSeekConfig.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<DeepSeekResponseDTO> chatCompletion(DeepSeekRequestDTO request) {
        log.info("调用DeepSeek API，请求元数据: {}", formatRequestForLog(request));
        return webClient.post()
                .uri(deepSeekConfig.getChatEndpoint())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DeepSeekResponseDTO.class)
                .retryWhen(Retry.backoff(deepSeekConfig.getMaxRetries(), Duration.ofMillis(400))
                        .filter(this::shouldRetry))
                .timeout(Duration.ofSeconds(deepSeekConfig.getTimeoutSeconds()))
                .doOnSuccess(response -> log.info("DeepSeek API调用成功"))
                .doOnError(error -> log.warn("DeepSeek API调用失败: {}", conciseMessage(error)));
    }

    public DeepSeekResponseDTO chatCompletionSync(DeepSeekRequestDTO request) {
        return circuitBreaker.execute("deepseek", () -> chatCompletion(request).block());
    }

    private boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            return statusCode == 429 || statusCode >= 500;
        }
        return true;
    }

    private String formatRequestForLog(DeepSeekRequestDTO request) {
        try {
            DeepSeekRequestDTO logRequest = new DeepSeekRequestDTO();
            logRequest.setModel(request.getModel());
            logRequest.setMaxTokens(request.getMaxTokens());
            logRequest.setTemperature(request.getTemperature());
            logRequest.setTopP(request.getTopP());
            logRequest.setThinking(request.getThinking());
            logRequest.setStream(request.isStream());
            if (request.getMessages() != null) {
                logRequest.setMessages(request.getMessages().stream()
                        .map(message -> new DeepSeekRequestDTO.MessageDTO(message.getRole(), "***"))
                        .toList());
            }
            return objectMapper.writeValueAsString(logRequest);
        } catch (JsonProcessingException ex) {
            return "request-metadata-unavailable";
        }
    }

    private String conciseMessage(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            return String.valueOf(ex.getStatusCode().value());
        }
        return throwable.getClass().getSimpleName();
    }
}
