package elm_bk.service;

import elm_bk.vo.AssistantCapabilityVO;
import elm_bk.vo.DishRecognitionVO;
import elm_bk.vo.VoiceOrderDraftVO;
import org.springframework.web.multipart.MultipartFile;

public interface MultimodalAssistantService {
    AssistantCapabilityVO capabilities();

    DishRecognitionVO recognizeDish(MultipartFile image);

    VoiceOrderDraftVO createVoiceOrderDraft(MultipartFile audio);
}
