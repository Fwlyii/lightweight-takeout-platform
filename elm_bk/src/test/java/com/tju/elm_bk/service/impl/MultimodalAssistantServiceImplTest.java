package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.adapter.ImageRecognitionAdapter;
import com.tju.elm_bk.adapter.SpeechAdapter;
import com.tju.elm_bk.config.MultimodalAiConfig;
import com.tju.elm_bk.dto.RecommendationRequestDTO;
import com.tju.elm_bk.service.AiRecommendationService;
import com.tju.elm_bk.utils.MediaFileValidator;
import com.tju.elm_bk.utils.VoiceOrderParser;
import com.tju.elm_bk.vo.AiRecommendationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultimodalAssistantServiceImplTest {
    private ImageRecognitionAdapter imageAdapter;
    private SpeechAdapter speechAdapter;
    private AiRecommendationService recommendationService;
    private MultimodalAssistantServiceImpl service;

    @BeforeEach
    void setUp() {
        imageAdapter = mock(ImageRecognitionAdapter.class);
        speechAdapter = mock(SpeechAdapter.class);
        recommendationService = mock(AiRecommendationService.class);
        MultimodalAiConfig config = new MultimodalAiConfig();
        config.setApiKey("test-key");
        service = new MultimodalAssistantServiceImpl(imageAdapter, speechAdapter, recommendationService,
                new MediaFileValidator(), new VoiceOrderParser(), config);
        when(imageAdapter.isAvailable()).thenReturn(true);
        when(speechAdapter.isAvailable()).thenReturn(true);
        when(imageAdapter.provider()).thenReturn("test-vision");
        when(speechAdapter.provider()).thenReturn("test-speech");
    }

    @Test
    void shouldDeduplicateImageCandidates() {
        MockMultipartFile image = new MockMultipartFile("image", "dish.jpg", "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00});
        AiRecommendationVO food = food(1L, "牛肉面");
        when(imageAdapter.recognize(any(), eq("image/jpeg"))).thenReturn(
                new ImageRecognitionAdapter.ImageRecognitionResult("牛肉面", List.of("牛肉面", "面食"), 0.9));
        when(recommendationService.recommend(anyString(), eq(null), eq(false))).thenReturn(List.of(food));

        var result = service.recognizeDish(image);

        assertEquals(1, result.candidates().size());
        assertEquals(1L, result.candidates().get(0).getFoodId());
    }

    @Test
    void shouldCreateEditableVoiceDraft() {
        MockMultipartFile audio = new MockMultipartFile("audio", "voice.webm", "audio/webm",
                new byte[]{0x1a, 0x45, (byte) 0xdf, (byte) 0xa3, 0x00});
        when(speechAdapter.transcribe(any(), eq("audio/webm"))).thenReturn(
                new SpeechAdapter.SpeechTranscriptionResult("帮我来两份牛肉面", "zh-CN"));
        when(recommendationService.recommend(any(RecommendationRequestDTO.class))).thenReturn(List.of(food(1L, "牛肉面")));

        var result = service.createVoiceOrderDraft(audio);

        assertEquals("牛肉面", result.query());
        assertEquals(2, result.quantity());
        assertEquals(1, result.candidates().size());
    }

    private AiRecommendationVO food(Long id, String name) {
        return new AiRecommendationVO(id, name, new BigDecimal("18.00"), null, 1L, "测试商家", "匹配");
    }
}
