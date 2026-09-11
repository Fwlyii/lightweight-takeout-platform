package com.tju.elm_bk.service;

import com.tju.elm_bk.vo.AssistantCapabilityVO;
import com.tju.elm_bk.vo.DishRecognitionVO;
import com.tju.elm_bk.vo.VoiceOrderDraftVO;
import org.springframework.web.multipart.MultipartFile;

public interface MultimodalAssistantService {
    AssistantCapabilityVO capabilities();

    DishRecognitionVO recognizeDish(MultipartFile image);

    VoiceOrderDraftVO createVoiceOrderDraft(MultipartFile audio);
}
