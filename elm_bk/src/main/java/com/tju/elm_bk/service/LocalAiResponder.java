package com.tju.elm_bk.service;

import com.tju.elm_bk.dto.AiChatRequestDTO;
import com.tju.elm_bk.utils.AiKnowledgeBaseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.stream.Collectors;

/** 不调用模型的本地帮助，只引用数据库中实际存在的数据。 */
@Component
@RequiredArgsConstructor
public class LocalAiResponder {
    private final AiKnowledgeBaseUtil knowledge;

    public String reply(AiChatRequestDTO request) {
        String message = request.getMessage().toLowerCase(Locale.ROOT);
        if (contains(message, "投诉", "人工", "转接")) {
            return "请打开相关订单详情核对信息，并联系商家或平台管理人员处理。当前对话不能直接转接人工，也不会自动创建投诉工单。";
        }
        if (contains(message, "你好", "hello")) {
            return "您好，我是小饿。您可以查询在售商家、菜品和自己的订单，也可以询问配送与支付规则。";
        }
        if (contains(message, "配送费", "起送价", "起送费")) {
            return "配送费和起送价由商家设置，以商家页与结算页为准。外送需满足起送条件；到店自取不收配送费。最终金额由服务器重新计算。";
        }
        if ("business".equals(request.getChatType()) || contains(message, "商家", "餐厅", "店铺")) {
            var businesses = knowledge.searchBusinesses("", 3);
            if (businesses.isEmpty()) return "暂无可推荐的在售商家，请稍后在首页重新查看。";
            return businesses.stream().map(knowledge::formatBusinessInfo).collect(Collectors.joining("\n"));
        }
        if ("food".equals(request.getChatType()) || contains(message, "菜", "美食", "推荐", "清淡")) {
            var foods = knowledge.searchFoods("", 3);
            if (foods.isEmpty()) return "暂无可推荐的在售菜品，请稍后查看商家菜单。";
            return foods.stream().map(knowledge::formatFoodInfo).collect(Collectors.joining("\n"));
        }
        if ("order".equals(request.getChatType()) || contains(message, "订单", "配送", "支付", "退款")) {
            var orders = knowledge.getRecentOrdersByUserId(request.getUserId(), 3);
            if (orders.isEmpty()) return "没有找到您的订单记录，请在订单列表核对。";
            return "您的最近订单：\n" + orders.stream().map(knowledge::formatOrderInfo).collect(Collectors.joining("\n"))
                    + "\n具体操作请进入对应订单详情，客服对话不会代您支付、取消或退款。";
        }
        return "我可以帮助您查看商家、菜品和订单信息。需要人工处理的问题，请从相关订单联系商家或平台管理人员。";
    }

    private boolean contains(String message, String... keywords) {
        for (String keyword : keywords) if (message.contains(keyword)) return true;
        return false;
    }
}
