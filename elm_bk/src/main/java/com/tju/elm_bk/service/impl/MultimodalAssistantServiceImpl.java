package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.adapter.ImageRecognitionAdapter;
import com.tju.elm_bk.adapter.SpeechAdapter;
import com.tju.elm_bk.config.MultimodalAiConfig;
import com.tju.elm_bk.dto.RecommendationRequestDTO;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.service.AiRecommendationService;
import com.tju.elm_bk.service.MultimodalAssistantService;
import com.tju.elm_bk.utils.MediaFileValidator;
import com.tju.elm_bk.utils.VoiceOrderParser;
import com.tju.elm_bk.vo.AiRecommendationVO;
import com.tju.elm_bk.vo.AssistantCapabilityVO;
import com.tju.elm_bk.vo.DishRecognitionVO;
import com.tju.elm_bk.vo.VoiceOrderDraftVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MultimodalAssistantServiceImpl implements MultimodalAssistantService {
    private final ImageRecognitionAdapter imageRecognitionAdapter;
    private final SpeechAdapter speechAdapter;
    private final AiRecommendationService recommendationService;
    private final MediaFileValidator mediaFileValidator;
    private final VoiceOrderParser voiceOrderParser;
    private final MultimodalAiConfig config;

    @Override
    public AssistantCapabilityVO capabilities() {
        return new AssistantCapabilityVO(true, imageRecognitionAdapter.isAvailable(), speechAdapter.isAvailable(),
                imageRecognitionAdapter.provider(), speechAdapter.provider());
    }

    @Override
    public DishRecognitionVO recognizeDish(MultipartFile image) {
        if (!imageRecognitionAdapter.isAvailable()) {
            throw new APIException("图片识菜未配置，请设置DASHSCOPE_API_KEY和DASHSCOPE_BASE_URL");
        }
        MediaFileValidator.ValidatedMedia media = mediaFileValidator.image(image, config.getMaxImageBytes());
        ImageRecognitionAdapter.ImageRecognitionResult result = imageRecognitionAdapter.recognize(media.content(), media.mimeType());
        Map<Long, AiRecommendationVO> candidates = new LinkedHashMap<>();
        for (String keyword : result.keywords()) {
            recommendationService.recommend(keyword, null, false)
                    .forEach(food -> candidates.putIfAbsent(food.getFoodId(), food));
            if (candidates.size() >= 6) break;
        }
        List<AiRecommendationVO> limited = candidates.values().stream().limit(6).toList();
        return new DishRecognitionVO(result.summary(), result.keywords(), result.confidence(), limited,
                imageRecognitionAdapter.provider());
    }

    @Override
    public VoiceOrderDraftVO createVoiceOrderDraft(MultipartFile audio) {
        if (!speechAdapter.isAvailable()) {
            throw new APIException("语音识别未配置，请设置DASHSCOPE_API_KEY和DASHSCOPE_BASE_URL");
        }
        MediaFileValidator.ValidatedMedia media = mediaFileValidator.audio(audio, config.getMaxAudioBytes());
        SpeechAdapter.SpeechTranscriptionResult result = speechAdapter.transcribe(media.content(), media.mimeType());
        if (result.transcript() == null || result.transcript().isBlank()) {
            throw new APIException("没有识别到有效语音，请靠近麦克风后重试");
        }
        VoiceOrderParser.ParsedVoiceOrder parsed = voiceOrderParser.parse(result.transcript());
        RecommendationRequestDTO recommendationRequest = new RecommendationRequestDTO();
        recommendationRequest.setQuery(parsed.query());
        recommendationRequest.setBudget(parsed.budget());
        recommendationRequest.setQuantity(parsed.quantity());
        List<AiRecommendationVO> candidates = recommendationService.recommend(recommendationRequest);
        return new VoiceOrderDraftVO(result.transcript(), parsed.query(), parsed.quantity(), parsed.specification(),
                parsed.budget(), candidates, speechAdapter.provider());
    }
}
