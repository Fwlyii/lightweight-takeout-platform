package elm_bk.service;

import elm_bk.dto.DeliveryExceptionCreateDTO;
import elm_bk.dto.DeliveryExceptionResolveDTO;
import elm_bk.entity.DeliveryException;
import elm_bk.entity.OrderStatusHistory;
import elm_bk.vo.DeliveryExceptionVO;
import elm_bk.vo.DeliveryTaskVO;
import elm_bk.vo.NavigationVO;

import java.util.List;

public interface DeliveryService {
    int countDeliveringTasks();

    List<DeliveryTaskVO> listAvailableTasks();

    List<DeliveryTaskVO> listMyTasks(Boolean active);

    DeliveryTaskVO getTask(Long taskId);

    NavigationVO getNavigation(Long taskId);

    DeliveryTaskVO getOrderDelivery(Long orderId);

    DeliveryTaskVO merchantAcceptOrder(Long orderId);
    DeliveryTaskVO merchantReadyOrder(Long orderId);

    void merchantRejectOrder(Long orderId);

    DeliveryTaskVO acceptTask(Long taskId);

    DeliveryTaskVO arriveStore(Long taskId);

    DeliveryTaskVO pickup(Long taskId);

    DeliveryTaskVO deliver(Long taskId);

    DeliveryTaskVO confirmReceipt(Long orderId);

    DeliveryException reportException(Long taskId, DeliveryExceptionCreateDTO dto);

    List<DeliveryExceptionVO> listExceptions(Integer status);

    DeliveryTaskVO resolveException(Long exceptionId, DeliveryExceptionResolveDTO dto);

    List<OrderStatusHistory> listOrderHistory(Long orderId);
}
