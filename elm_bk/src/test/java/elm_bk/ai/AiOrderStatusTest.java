package elm_bk.ai;

import elm_bk.constant.OrderStatus;
import elm_bk.entity.Order;
import elm_bk.utils.AiKnowledgeBaseUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiOrderStatusTest {
    @ParameterizedTest @EnumSource(OrderStatus.class)
    void aiUsesTheSameOrderStatusLabelsAsTheRestOfThePlatform(OrderStatus status) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderState(status.getCode());
        order.setOrderTotal(new BigDecimal("20.00"));
        assertTrue(new AiKnowledgeBaseUtil(null, null, null, null).formatOrderInfo(order).contains(status.getLabel()));
    }
}
