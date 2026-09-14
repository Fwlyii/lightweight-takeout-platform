package com.tju.elm_bk.adapter.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tju.elm_bk.adapter.ImageRecognitionAdapter;
import com.tju.elm_bk.client.DashScopeMultimodalClient;
import com.tju.elm_bk.config.MultimodalAiConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class DashScopeImageRecognitionAdapterTest {
    private final DashScopeImageRecognitionAdapter adapter = new DashScopeImageRecognitionAdapter(
            mock(DashScopeMultimodalClient.class), new MultimodalAiConfig(), new ObjectMapper());

    @Test
    void shouldParseJsonInsideMarkdownFence() {
        ImageRecognitionAdapter.ImageRecognitionResult result = adapter.parseResult("""
                ```json
                {"summary":"可能是牛肉面","keywords":["牛肉面","面食","牛肉"],"confidence":0.86}
                ```
                """);

        assertEquals("可能是牛肉面", result.summary());
        assertEquals(3, result.keywords().size());
        assertEquals(0.86, result.confidence());
    }
}
