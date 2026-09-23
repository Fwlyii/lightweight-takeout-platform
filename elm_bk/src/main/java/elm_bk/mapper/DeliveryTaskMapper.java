package elm_bk.mapper;

import elm_bk.entity.DeliveryException;
import elm_bk.entity.DeliveryTask;
import elm_bk.vo.DeliveryExceptionVO;
import elm_bk.vo.DeliveryTaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeliveryTaskMapper {
    @Select("SELECT b.longitude,b.latitude FROM business b JOIN orders o ON o.business_id=b.id WHERE o.id=#{orderId}")
    elm_bk.entity.GeoPoint merchantPoint(Long orderId);
    @Select("SELECT a.longitude,a.latitude FROM delivery_address a JOIN orders o ON o.address_id=a.id WHERE o.id=#{orderId}")
    elm_bk.entity.GeoPoint customerPoint(Long orderId);
    @Select("SELECT COUNT(*) FROM delivery_task WHERE task_status = 'DELIVERING'")
    int countDeliveringTasks();

    void insertTask(DeliveryTask task);

    DeliveryTask selectById(Long id);

    DeliveryTask selectByOrderId(Long orderId);

    DeliveryTaskVO selectViewById(Long id);

    DeliveryTaskVO selectViewByOrderId(Long orderId);

    List<DeliveryTaskVO> listAvailable();

    List<DeliveryTaskVO> listByRider(@Param("riderUserId") Long riderUserId,
                                     @Param("active") Boolean active);

    int acceptTask(@Param("taskId") Long taskId, @Param("riderUserId") Long riderUserId);

    int markArrivedStore(@Param("taskId") Long taskId, @Param("riderUserId") Long riderUserId);

    int markPickedUp(@Param("taskId") Long taskId, @Param("riderUserId") Long riderUserId);

    int markDelivered(@Param("taskId") Long taskId, @Param("riderUserId") Long riderUserId);

    int markCompleted(@Param("taskId") Long taskId);

    int autoCompleteDelivered(@Param("orderId") Long orderId);

    int markException(@Param("taskId") Long taskId, @Param("riderUserId") Long riderUserId);

    int resumeFromException(@Param("taskId") Long taskId, @Param("targetStatus") String targetStatus);

    int reassignFromException(@Param("taskId") Long taskId);

    int cancelFromException(@Param("taskId") Long taskId);

    int cancelWaitingTask(@Param("taskId") Long taskId);

    int countActiveByRider(@Param("riderUserId") Long riderUserId);

    void insertException(DeliveryException deliveryException);

    DeliveryException selectExceptionById(Long id);

    DeliveryException selectOpenExceptionByTask(Long taskId);

    List<DeliveryExceptionVO> listExceptions(@Param("status") Integer status);

    int resolveException(@Param("id") Long id,
                         @Param("action") String action,
                         @Param("note") String note,
                         @Param("resolverUserId") Long resolverUserId);
}
