package com.tju.elm_bk.service;

import com.tju.elm_bk.constant.OrderStatus;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.mapper.OrdersMapper;
import com.tju.elm_bk.vo.BusinessSearchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Homepage, search and favorites share the exact same presentation rules. */
@Service
@RequiredArgsConstructor
public class BusinessPresentationService {
    private final OrdersMapper ordersMapper;
    private final CurrentUserService currentUserService;
    private final BusinessRecommendationPolicy recommendationPolicy;

    public void enrich(List<BusinessSearchVO> businesses) {
        Set<Long> recentPurchaseIds = getRecentPurchaseIds();
        for (BusinessSearchVO business : businesses) {
            if (business.getScore() != null) {
                business.setScore(business.getScore().setScale(2, RoundingMode.HALF_UP));
            }
            recommendationPolicy.enrich(business, recentPurchaseIds.contains(business.getId()));
        }
    }

    private static final int RECENT_PURCHASE_DAYS = 30;

    private Set<Long> getRecentPurchaseIds() {
        Set<Long> ids = new HashSet<>();
        try {
            Long userId = currentUserService.optionalUser().map(User::getId).orElse(null);
            if (userId == null) return ids;
            LocalDateTime cutoff = LocalDateTime.now().minusDays(RECENT_PURCHASE_DAYS);
            List<com.tju.elm_bk.entity.Order> orders = ordersMapper.selectRecentOrdersByUserId(userId, 100);
            if (orders == null) return ids;
            orders.stream()
                    .filter(order -> order.getBusinessId() != null)
                    .filter(order -> order.getOrderState() == null || !Set.of(
                            OrderStatus.CANCELLED.getCode(),
                            OrderStatus.DELIVERY_EXCEPTION.getCode()).contains(order.getOrderState()))
                    .filter(order -> order.getOrderDate() == null || !order.getOrderDate().isBefore(cutoff))
                    .forEach(order -> ids.add(order.getBusinessId()));
        } catch (Exception ignored) {
            // 推荐是增强能力，订单历史查询失败不应阻塞首页浏览。
        }
        return ids;
    }

}
