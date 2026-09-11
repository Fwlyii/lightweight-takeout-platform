package com.tju.elm_bk.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tju.elm_bk.dto.AiChatRequestDTO;
import com.tju.elm_bk.dto.AssistantMessageRequestDTO;
import com.tju.elm_bk.entity.AiChatHistory;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.AiChatHistoryMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.service.AiChatService;
import com.tju.elm_bk.service.AiRecommendationService;
import com.tju.elm_bk.service.StructuredAssistantService;
import com.tju.elm_bk.utils.SecurityUtils;
import com.tju.elm_bk.vo.AiChatResponseVO;
import com.tju.elm_bk.vo.AiRecommendationVO;
import com.tju.elm_bk.vo.AssistantMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StructuredAssistantServiceImpl implements StructuredAssistantService {
    private final AiChatService aiChatService;
    private final AiRecommendationService recommendationService;
    private final AiChatHistoryMapper chatHistoryMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AssistantMessageVO message(AssistantMessageRequestDTO request) {
        long start = System.currentTimeMillis();
        User user = currentUser();
        String sessionId = request.getSessionId() == null || request.getSessionId().isBlank()
                ? UUID.randomUUID().toString() : request.getSessionId().trim();
        assertSessionOwner(sessionId, user.getId());
        String message = request.getMessage().trim();
        Intent intent = detectIntent(message);

        if (intent == Intent.RECOMMENDATION) {
            List<AiRecommendationVO> candidates = recommendationService.recommend(message, request.getBudget(),
                    !Boolean.FALSE.equals(request.getUsePreferences()));
            String reply = candidates.isEmpty()
                    ? "暂时没有符合条件的在售商品，请调整关键词或预算。"
                    : "已按当前库存、销量和你授权的偏好找到以下商品，请确认后再加入购物车。";
            long duration = System.currentTimeMillis() - start;
            saveHistory(user.getId(), sessionId, message, reply, intent, duration, candidates.size());
            return new AssistantMessageVO(intent.name(), reply, sessionId, candidates, !candidates.isEmpty(), duration);
        }

        if (intent == Intent.RULE) {
            String reply = ruleReply(message);
            long duration = System.currentTimeMillis() - start;
            saveHistory(user.getId(), sessionId, message, reply, intent, duration, 0);
            return new AssistantMessageVO(intent.name(), reply, sessionId, List.of(), false, duration);
        }

        AiChatRequestDTO chatRequest = new AiChatRequestDTO();
        chatRequest.setMessage(message);
        chatRequest.setUserId(user.getId());
        chatRequest.setSessionId(sessionId);
        chatRequest.setChatType(intent == Intent.ORDER ? "order" : "general");
        AiChatResponseVO response = aiChatService.chat(chatRequest);
        return new AssistantMessageVO(intent.name(), response.getMessage(), response.getSessionId(), List.of(),
                Boolean.TRUE.equals(response.getNeedConfirmation()), response.getProcessingTime() == null ? System.currentTimeMillis() - start : response.getProcessingTime());
    }

    private User currentUser() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new APIException(ResultCodeEnum.UNAUTHORIZED));
        User user = userMapper.findByUsername(username);
        if (user == null) throw new APIException(ResultCodeEnum.UNAUTHORIZED);
        return user;
    }

    private void assertSessionOwner(String sessionId, Long userId) {
        Long ownerId = chatHistoryMapper.findUserIdBySessionId(sessionId);
        if (ownerId != null && !ownerId.equals(userId)) {
            throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
        }
    }

    private Intent detectIntent(String message) {
        String normalized = message.toLowerCase();
        if (containsAny(normalized, "规则", "退款", "取消", "会员", "权益", "优惠", "多久", "时效", "客服")) {
            return Intent.RULE;
        }
        if (containsAny(normalized, "订单", "催单", "配送状态", "送到", "骑手", "物流")) {
            return Intent.ORDER;
        }
        if (containsAny(normalized, "推荐", "想吃", "吃什么", "菜品", "美食", "商家", "餐厅", "预算", "元以内",
                "清淡", "低脂", "辣", "甜", "面", "饭", "粉", "粥", "汤", "茶", "咖啡", "汉堡", "披萨")) {
            return Intent.RECOMMENDATION;
        }
        return Intent.GENERAL;
    }

    private String ruleReply(String message) {
        if (containsAny(message, "退款", "取消")) {
            return "退款和取消是否可用取决于订单当前状态，请以订单详情页可执行的操作为准；无法操作时请通过平台客服处理。";
        }
        if (containsAny(message, "多久", "时效", "配送")) {
            return "配送时间会受商家出餐、骑手接单、距离和路况影响，请以订单详情页的实时状态为准。";
        }
        if (containsAny(message, "会员", "权益", "优惠")) {
            return "会员权益和优惠以资产与优惠券页面当前展示的可用项目、门槛和有效期为准。";
        }
        return "平台规则以相关业务页面当前展示和可执行操作为准；涉及具体订单时，请提供订单号查询本人订单。";
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) return true;
        }
        return false;
    }

    private void saveHistory(Long userId, String sessionId, String userMessage, String reply,
                             Intent intent, long processingTime, int candidateCount) {
        try {
            AiChatHistory history = new AiChatHistory();
            history.setUserId(userId);
            history.setSessionId(sessionId);
            history.setUserMessage(userMessage);
            history.setAiResponse(reply);
            history.setChatType(intent.name().toLowerCase());
            history.setProcessingTime(processingTime);
            history.setCreateTime(LocalDateTime.now());
            history.setCreator(userId);
            history.setIsDeleted(false);
            history.setContextData(objectMapper.writeValueAsString(Map.of(
                    "responseType", "structured",
                    "intent", intent.name(),
                    "candidateCount", candidateCount,
                    "needConfirmation", candidateCount > 0)));
            chatHistoryMapper.insert(history);
        } catch (Exception ex) {
            log.warn("结构化助手历史保存失败: userId={}, sessionId={}", userId, sessionId);
        }
    }

    private enum Intent {
        RECOMMENDATION,
        ORDER,
        RULE,
        GENERAL
    }
}
