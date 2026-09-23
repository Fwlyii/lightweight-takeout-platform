package elm_bk.service.impl;

import elm_bk.dto.AiChatRequestDTO;
import elm_bk.dto.AssistantMessageRequestDTO;
import elm_bk.entity.User;
import elm_bk.service.AiChatHistoryService;
import elm_bk.service.CurrentUserService;
import elm_bk.service.AiChatService;
import elm_bk.service.AiRecommendationService;
import elm_bk.service.StructuredAssistantService;
import elm_bk.vo.AiChatResponseVO;
import elm_bk.vo.AiRecommendationVO;
import elm_bk.vo.AssistantMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StructuredAssistantServiceImpl implements StructuredAssistantService {
    private final AiChatService aiChatService;
    private final AiRecommendationService recommendationService;
    private final AiChatHistoryService historyService;
    private final CurrentUserService currentUser;

    @Override
    public AssistantMessageVO message(AssistantMessageRequestDTO request) {
        long start = System.currentTimeMillis();
        User user = currentUser.requireUser();
        String sessionId = request.getSessionId() == null || request.getSessionId().isBlank()
                ? UUID.randomUUID().toString() : request.getSessionId().trim();
        historyService.requireWritableSession(sessionId);
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
        AiChatRequestDTO request = new AiChatRequestDTO();
        request.setUserId(userId);
        request.setMessage(userMessage);
        request.setSessionId(sessionId);
        request.setChatType(intent.name().toLowerCase());
        AiChatResponseVO response = new AiChatResponseVO();
        response.setSessionId(sessionId);
        response.setMessage(reply);
        response.setSource("local");
        response.setResponseType("structured");
        response.setNeedConfirmation(candidateCount > 0);
        response.setProcessingTime(processingTime);
        historyService.save(request, response);
    }

    private enum Intent {
        RECOMMENDATION,
        ORDER,
        RULE,
        GENERAL
    }
}
