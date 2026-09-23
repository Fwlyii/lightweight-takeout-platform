package elm_bk.controller;

import elm_bk.dto.OrderDTO;
import elm_bk.exception.APIException;
import elm_bk.result.HttpResult;
import elm_bk.result.ResultCodeEnum;
import elm_bk.service.OrderService;
import elm_bk.vo.OrderItemDetailVO;
import elm_bk.vo.OrderItemVO;
import elm_bk.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@Tag(name="管理订单")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "获取用户订单列表",description = "旧数据结构兼容接口", deprecated = true)
    public HttpResult<List<OrderVO>> listOrdersByUserId (Long userId) {
        return HttpResult.success(orderService.getCustomerOrderList(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据id获取用户订单",description = "旧数据结构兼容接口", deprecated = true)
    public HttpResult<OrderVO> getOrderById(@PathVariable Long id) {
        return HttpResult.success(orderService.getOrderById(id));
    }

    @PostMapping
    @Operation(summary = "新增订单",description = "旧数据结构兼容接口", deprecated = true)
    public HttpResult<OrderVO> addOrders(@RequestBody OrderDTO orderDTO) {
        return HttpResult.success(orderService.addOrder(orderDTO));
    }

    @GetMapping("/list/business")
    @Operation(summary = "根据商家和状态获取订单列表")
    public HttpResult<List<OrderItemDetailVO>> listOrdersByBusiness(@RequestParam(required = false) Long businessId, @RequestParam(required = false) Integer orderState) {
        return HttpResult.success(orderService.getOrderItemListByBusiness(businessId,orderState));
    }

    @GetMapping("/list/user")
    @Operation(summary = "获取用户自己的相应状态的订单列表")
    public HttpResult<List<OrderItemVO>> listOrdersByUser(@RequestParam(required = false) Integer orderState) {
        return HttpResult.success(orderService.getOrderItemListByUser(orderState));
    }

    @GetMapping("/detail")
    @Operation(summary = "获取订单详情")
    public HttpResult<OrderItemDetailVO> listOrdersByUser(@RequestParam Long orderId) {
        return HttpResult.success(orderService.getOrderItemDetail(orderId));
    }

    @PutMapping("/status")
    @Operation(summary = "支付或取消订单", description = "只允许目标状态1（待商家接单）或8（已取消）；配送流转请使用/api/v1专用接口")
    public HttpResult<Long> setOrderStatus(@RequestParam Integer orderState,
                                           @RequestParam Long orderId,
                                           @RequestParam(required = false) String paymentMethod,
                                           @RequestParam(required = false, defaultValue = "0") Integer pointsToUse,
                                           @RequestParam(required = false) Long couponId) {
        return HttpResult.success(orderService.setOrderState(orderId, orderState, paymentMethod, pointsToUse, couponId));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交订单", description = "使用幂等键避免重复点击创建多笔订单")
    public HttpResult<Long> orderSubmit(@RequestParam Long businessId,
                                        @RequestParam(required = false) Long addressId,
                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                        @RequestParam(required = false) String requestId,
                                        @RequestParam(required = false, defaultValue = "delivery") String serviceMode,
                                        @RequestParam(required = false) String foodIds) {
        // 兼容旧前端：没有请求头时也接受 requestId 查询参数。
        List<Long> selectedFoodIds = parseFoodIds(foodIds);
        return HttpResult.success(orderService.orderSubmit(businessId, addressId,
                idempotencyKey == null ? requestId : idempotencyKey, serviceMode, selectedFoodIds));
    }

    private List<Long> parseFoodIds(String foodIds) {
        if (foodIds == null) return null;
        if (foodIds.length() > 2000) throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        try {
            List<Long> ids = Arrays.stream(foodIds.split(",", -1))
                    .map(String::trim)
                    .map(value -> {
                        if (!value.matches("[1-9][0-9]*")) throw new NumberFormatException("invalid item id");
                        return Long.valueOf(value);
                    })
                    .distinct()
                    .collect(Collectors.toList());
            if (ids.isEmpty()) throw new NumberFormatException("empty");
            if (ids.size() > 100) throw new NumberFormatException("too many items");
            return ids;
        } catch (NumberFormatException ex) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
    }

}
