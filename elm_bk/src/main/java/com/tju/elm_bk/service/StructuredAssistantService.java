package com.tju.elm_bk.service;

import com.tju.elm_bk.dto.AssistantMessageRequestDTO;
import com.tju.elm_bk.vo.AssistantMessageVO;

public interface StructuredAssistantService {
    AssistantMessageVO message(AssistantMessageRequestDTO request);
}
