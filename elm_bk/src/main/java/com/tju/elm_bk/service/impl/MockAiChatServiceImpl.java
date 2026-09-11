package com.tju.elm_bk.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tju.elm_bk.constant.OrderStatus;
import com.tju.elm_bk.dto.AiChatRequestDTO;
import com.tju.elm_bk.entity.AiChatHistory;
import com.tju.elm_bk.entity.Order;
import com.tju.elm_bk.mapper.AiChatHistoryMapper;
import com.tju.elm_bk.service.AiChatService;
import com.tju.elm_bk.service.AiRecommendationService;
import com.tju.elm_bk.utils.AiKnowledgeBaseUtil;
import com.tju.elm_bk.vo.AiChatHistoryVO;
import com.tju.elm_bk.vo.AiChatResponseVO;
import com.tju.elm_bk.vo.AiRecommendationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockAiChatServiceImpl implements AiChatService {
    private static final Pattern ORDER_ID = Pattern.compile("(?:订单号?[：:]?|订单ID[：:]?|查(?:一下)?)(\\d+)");

    private final AiKnowledgeBaseUtil knowledgeBaseUtil;
    private final AiRecommendationService recommendationService;
    private final AiChatHistoryMapper chatHistoryMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AiChatResponseVO chat(AiChatRequestDTO request) {
        long start = System.currentTimeMillis();
        String sessionId = request.getSessionId() == null || request.getSessionId().isBlank()
                ? UUID.randomUUID().toString() : request.getSessionId().trim();
        String reply = generateResponse(request);
        long duration = System.currentTimeMillis() - start;

        AiChatResponseVO response = new AiChatResponseVO();
        response.setMessage(reply);
        response.setSessionId(sessionId);
        response.setResponseType("text");
        response.setResponseTime(LocalDateTime.now());
        response.setProcessingTime(duration);
        saveChatHistory(request, response, duration);
        log.info("本地AI降级回复完成: userId={}, sessionId={}, processingTime={}ms",
                request.getUserId(), sessionId, duration);
        return response;
    }

    private String generateResponse(AiChatRequestDTO request) {
        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        String normalized = message.toLowerCase();
        if (containsAny(normalized, "订单", "配送", "催单", "物流") || "order".equals(request.getChatType())) {
            return orderResponse(request.getUserId(), message);
        }
        if (containsAny(normalized, "推荐", "想吃", "菜", "美食", "餐厅", "商家", "清淡", "辣")
                || "food".equals(request.getChatType()) || "business".equals(request.getChatType())) {
            return recommendationResponse(message);
        }
        if (containsAny(normalized, "你好", "hello", "hi")) {
            return "你好，我可以根据平台当前在售商品提供推荐，也可以查询你本人的订单状态。";
        }
        return "当前未配置外部大模型。我仍可提供真实在售商品推荐和本人订单查询，请直接描述菜品需求或订单号。";
    }

    private String recommendationResponse(String query) {
        List<AiRecommendationVO> candidates = recommendationService.recommend(query, null, true);
        if (candidates.isEmpty()) {
            return "当前没有找到符合条件的在售商品，请换一个菜名、品类或预算重试。";
        }
        StringBuilder reply = new StringBuilder("根据当前库存与平台数据找到：\n");
        for (int i = 0; i < candidates.size(); i++) {
            AiRecommendationVO item = candidates.get(i);
            reply.append(i + 1).append(". ").append(item.getFoodName())
                    .append("（").append(item.getBusinessName()).append("）¥")
                    .append(item.getPrice()).append('\n');
        }
        reply.append("请在商品卡片中确认后加入购物车。");
        return reply.toString();
    }

    private String orderResponse(Long userId, String message) {
        if (userId == null) return "登录后才能查询本人订单。";
        Matcher matcher = ORDER_ID.matcher(message);
        if (matcher.find()) {
            Long orderId = Long.valueOf(matcher.group(1));
            Order order = knowledgeBaseUtil.getOrderById(orderId);
            if (order == null || !userId.equals(order.getCustomerId())) {
                return "未找到该订单，或该订单不属于当前账号。";
            }
            return formatOrder(order);
        }
        List<Order> orders = knowledgeBaseUtil.getRecentOrdersByUserId(userId, 5);
        if (orders.isEmpty()) return "当前账号暂无订单记录。";
        StringBuilder reply = new StringBuilder("最近订单状态：\n");
        orders.forEach(order -> reply.append("- ").append(formatOrder(order)).append('\n'));
        return reply.toString().trim();
    }

    private String formatOrder(Order order) {
        String state;
        try {
            state = OrderStatus.fromCode(order.getOrderState()).getLabel();
        } catch (Exception ignored) {
            state = "状态待确认";
        }
        return "订单 #" + order.getId() + "：" + state + "，金额 ¥" + order.getOrderTotal();
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) if (value.contains(keyword)) return true;
        return false;
    }

    private void saveChatHistory(AiChatRequestDTO request, AiChatResponseVO response, long processingTime) {
        try {
            AiChatHistory history = new AiChatHistory();
            history.setUserId(request.getUserId());
            history.setSessionId(response.getSessionId());
            history.setUserMessage(request.getMessage());
            history.setAiResponse(response.getMessage());
            history.setChatType(request.getChatType());
            history.setProcessingTime(processingTime);
            history.setCreateTime(LocalDateTime.now());
            history.setCreator(request.getUserId());
            history.setIsDeleted(false);
            history.setContextData(objectMapper.writeValueAsString(Map.of(
                    "responseType", response.getResponseType(), "mockService", true)));
            chatHistoryMapper.insert(history);
        } catch (Exception ex) {
            log.warn("本地AI历史保存失败: userId={}, sessionId={}", request.getUserId(), response.getSessionId());
        }
    }

    @Override
    public List<AiChatHistoryVO> getChatHistory(Long userId, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 || size > 50 ? 20 : size;
        return chatHistoryMapper.selectByUserId(userId, safeSize, (safePage - 1) * safeSize).stream()
                .map(this::toVO).toList();
    }

    @Override
    public List<AiChatHistoryVO> getChatHistoryBySession(String sessionId) {
        return chatHistoryMapper.selectBySessionId(sessionId).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public Boolean deleteChatHistory(Long historyId, Long userId) {
        return chatHistoryMapper.deleteById(historyId, userId) > 0;
    }

    @Override
    @Transactional
    public Boolean cleanOldChatHistory(Long userId, Integer keepCount) {
        int safeKeepCount = keepCount == null || keepCount < 10 ? 50 : keepCount;
        return chatHistoryMapper.cleanOldRecords(userId, safeKeepCount) >= 0;
    }

    private AiChatHistoryVO toVO(AiChatHistory history) {
        AiChatHistoryVO vo = new AiChatHistoryVO();
        BeanUtils.copyProperties(history, vo);
        return vo;
    }
}
