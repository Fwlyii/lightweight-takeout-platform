package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.AssistantMessageRequestDTO;
import com.tju.elm_bk.dto.RecommendationRequestDTO;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.AiRecommendationService;
import com.tju.elm_bk.service.MultimodalAssistantService;
import com.tju.elm_bk.service.StructuredAssistantService;
import com.tju.elm_bk.vo.AiRecommendationVO;
import com.tju.elm_bk.vo.AssistantCapabilityVO;
import com.tju.elm_bk.vo.AssistantMessageVO;
import com.tju.elm_bk.vo.DishRecognitionVO;
import com.tju.elm_bk.vo.VoiceOrderDraftVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "多模态智能交互")
public class AssistantController {
    private final MultimodalAssistantService multimodalAssistantService;
    private final AiRecommendationService recommendationService;
    private final StructuredAssistantService structuredAssistantService;

    @GetMapping("/assistant/capabilities")
    @Operation(summary = "查询智能交互能力和模型配置状态")
    public HttpResult<AssistantCapabilityVO> capabilities() {
        return HttpResult.success(multimodalAssistantService.capabilities());
    }

    @PostMapping("/assistant/messages")
    @Operation(summary = "发送结构化助手消息并返回真实商品候选")
    public HttpResult<AssistantMessageVO> message(@Valid @RequestBody AssistantMessageRequestDTO request) {
        return HttpResult.success(structuredAssistantService.message(request));
    }

    @PostMapping("/recommendations")
    @Operation(summary = "根据自然语言、预算和授权偏好推荐真实在售商品")
    public HttpResult<List<AiRecommendationVO>> recommendations(
            @Valid @RequestBody(required = false) RecommendationRequestDTO request) {
        RecommendationRequestDTO safeRequest = request == null ? new RecommendationRequestDTO() : request;
        return HttpResult.success(recommendationService.recommend(safeRequest));
    }

    @PostMapping(value = "/dish-recognitions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "识别菜品图片并返回可确认的真实商品候选")
    public HttpResult<DishRecognitionVO> recognizeDish(@RequestPart("image") MultipartFile image) {
        return HttpResult.success(multimodalAssistantService.recognizeDish(image));
    }

    @PostMapping(value = "/voice-order-drafts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "转写语音并生成可编辑的点餐草稿")
    public HttpResult<VoiceOrderDraftVO> createVoiceOrderDraft(@RequestPart("audio") MultipartFile audio) {
        return HttpResult.success(multimodalAssistantService.createVoiceOrderDraft(audio));
    }
}
