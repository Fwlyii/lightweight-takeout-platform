package elm_bk.service;

import elm_bk.dto.AssistantMessageRequestDTO;
import elm_bk.vo.AssistantMessageVO;

public interface StructuredAssistantService {
    AssistantMessageVO message(AssistantMessageRequestDTO request);
}
