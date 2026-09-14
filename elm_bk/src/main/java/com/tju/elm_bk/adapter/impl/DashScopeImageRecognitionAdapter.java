package com.tju.elm_bk.adapter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tju.elm_bk.adapter.ImageRecognitionAdapter;
import com.tju.elm_bk.client.DashScopeMultimodalClient;
import com.tju.elm_bk.config.MultimodalAiConfig;
import com.tju.elm_bk.exception.APIException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
@Profile("!ai-mock")
@RequiredArgsConstructor
public class DashScopeImageRecognitionAdapter implements ImageRecognitionAdapter {
    private static final String PROMPT = """
            识别图片中的主要菜品。只返回JSON，不要Markdown：
            {"summary":"简短识别说明","keywords":["最可能的菜名","食材或品类"],"confidence":0.0}
            keywords最多5个，confidence范围0到1。无法判断时keywords返回空数组并说明原因。
            """;

    private final DashScopeMultimodalClient client;
    private final MultimodalAiConfig config;
    private final ObjectMapper objectMapper;

    @Override
    public ImageRecognitionResult recognize(byte[] content, String mimeType) {
        String dataUri = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(content);
        Map<String, Object> request = Map.of(
                "model", config.getVisionModel(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "image_url", "image_url", Map.of("url", dataUri)),
                                Map.of("type", "text", "text", PROMPT)))),
                "temperature", 0.1,
                "max_tokens", 350
        );
        String text = client.extractText(client.chatCompletion(request));
        return parseResult(text);
    }

    ImageRecognitionResult parseResult(String text) {
        try {
            String json = extractJson(text);
            JsonNode root = objectMapper.readTree(json);
            LinkedHashSet<String> keywords = new LinkedHashSet<>();
            root.path("keywords").forEach(item -> {
                String keyword = item.asText("").trim();
                if (!keyword.isEmpty() && keyword.length() <= 30) keywords.add(keyword);
            });
            String summary = root.path("summary").asText("已完成图片识别").trim();
            double confidence = Math.max(0, Math.min(1, root.path("confidence").asDouble(0)));
            return new ImageRecognitionResult(summary, new ArrayList<>(keywords).stream().limit(5).toList(), confidence);
        } catch (Exception ex) {
            throw new APIException("图片已识别，但模型结果格式异常，请重新上传或手动输入菜名");
        }
    }

    private String extractJson(String text) {
        int start = text == null ? -1 : text.indexOf('{');
        int end = text == null ? -1 : text.lastIndexOf('}');
        if (start < 0 || end <= start) throw new IllegalArgumentException("missing json");
        return text.substring(start, end + 1);
    }

    @Override
    public boolean isAvailable() {
        return config.isConfigured();
    }

    @Override
    public String provider() {
        return "DashScope/" + config.getVisionModel();
    }
}
