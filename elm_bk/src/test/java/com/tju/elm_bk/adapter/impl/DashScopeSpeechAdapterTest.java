package com.tju.elm_bk.adapter.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tju.elm_bk.client.DashScopeMultimodalClient;
import com.tju.elm_bk.config.MultimodalAiConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashScopeSpeechAdapterTest {
    @Test
    @SuppressWarnings("unchecked")
    void shouldUseOpenAiCompatibleInputAudioObject() {
        DashScopeMultimodalClient client = mock(DashScopeMultimodalClient.class);
        MultimodalAiConfig config = new MultimodalAiConfig();
        DashScopeSpeechAdapter adapter = new DashScopeSpeechAdapter(client, config);
        when(client.chatCompletion(any())).thenReturn(new ObjectMapper().createObjectNode());
        when(client.extractText(any())).thenReturn("两份牛肉面");

        adapter.transcribe(new byte[]{1, 2, 3}, "audio/webm");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client).chatCompletion(captor.capture());
        Map<String, Object> request = captor.getValue();
        List<Map<String, Object>> messages = (List<Map<String, Object>>) request.get("messages");
        List<Map<String, Object>> content = (List<Map<String, Object>>) messages.get(0).get("content");
        Object inputAudio = content.get(0).get("input_audio");

        Map<String, Object> audio = assertInstanceOf(Map.class, inputAudio);
        assertTrue(String.valueOf(audio.get("data")).startsWith("data:audio/webm;base64,"));
        assertEquals("zh", ((Map<String, Object>) request.get("asr_options")).get("language"));
    }
}
