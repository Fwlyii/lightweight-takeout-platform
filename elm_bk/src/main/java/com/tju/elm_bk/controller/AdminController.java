package com.tju.elm_bk.controller;

import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.exception.APIException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "管理端数据看板", description = "管理端数据看板")
@RequiredArgsConstructor
public class AdminController {
    private final UserMapper userMapper;
    private final BusinessMapper businessMapper;
    private final OrdersMapper ordersMapper;
    private final com.tju.elm_bk.service.CurrentUserService currentUser;

    @GetMapping("/users")
    public HttpResult<java.util.List<com.tju.elm_bk.vo.AdminUserVO>> users(
            @RequestParam(defaultValue = "0") int status,
            @RequestParam(defaultValue = "") String keyword) {
        currentUser.requireUser();
        if (status < 0 || status > 2 || keyword.length() > 100) throw new APIException("查询条件无效");
        return HttpResult.success(userMapper.listAdminUsers(status, keyword.trim()));
    }

    @org.springframework.web.bind.annotation.PutMapping("/users/{userId}/status")
    @org.springframework.transaction.annotation.Transactional
    public HttpResult<Void> updateStatus(@org.springframework.web.bind.annotation.PathVariable Long userId,
            @RequestParam boolean activated) {
        Long operatorId = currentUser.requireUserId();
        userMapper.lockAccount(userId);
        var account = userMapper.findByUserIdWithAuthorities(userId);
        if (account == null || Boolean.TRUE.equals(account.getIsDeleted())) throw new APIException("用户不存在");
        if (!activated && (operatorId.equals(userId) || currentUser.isAdmin(account))) {
            throw new APIException("不能禁用自己或管理员账户");
        }
        account.setActivated(activated);
        userMapper.updateActivated(account);
        return HttpResult.success();
    }

    @GetMapping("/countUser")
    @Operation(summary = "获取总用户数", description = "获取总用户数")
    public HttpResult<Integer> countUser() {
        return HttpResult.success(userMapper.count());
    }

    @GetMapping("/countBusiness")
    @Operation(summary = "获取总店铺数", description = "获取总店铺数")
    public HttpResult<Integer> countBusiness() {
        return HttpResult.success(businessMapper.count());
    }

    @GetMapping("/countPrice")
    @Operation(summary = "获取总营业额", description = "获取总营业额")
    public HttpResult<Double> countPrice() {
        return HttpResult.success(ordersMapper.countPrice());
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<Map<String, Object>> statistics(@RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate end = to == null ? LocalDate.now().plusDays(1) : to.plusDays(1);
        return HttpResult.success(ordersMapper.aggregateStats(start.atStartOfDay(), end.atStartOfDay()));
    }

    @GetMapping(value = "/statistics/export", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> exportStatistics(@RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate end = to == null ? LocalDate.now().plusDays(1) : to.plusDays(1);
        Map<String, Object> data = ordersMapper.aggregateStats(start.atStartOfDay(), end.atStartOfDay());
        String csv = "指标,数值\n订单总数," + value(data, "orderCount") + "\n完成订单," + value(data, "completedCount")
                + "\n取消订单," + value(data, "cancelledCount") + "\n异常订单," + value(data, "exceptionCount")
                + "\n有效营业额," + value(data, "revenue") + "\n";
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statistics.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body("\uFEFF" + csv);
    }

    private Object value(Map<String, Object> data, String key) {
        return data.getOrDefault(key, 0);
    }
}
