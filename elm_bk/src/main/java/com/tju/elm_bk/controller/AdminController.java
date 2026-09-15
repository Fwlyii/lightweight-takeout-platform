package com.tju.elm_bk.controller;

import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.result.HttpResult;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "管理端数据看板", description = "管理端数据看板")
@RequiredArgsConstructor
public class AdminController {
    private final UserMapper userMapper;
    private final BusinessMapper businessMapper;
    private final OrdersMapper ordersMapper;

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
        Map<String, Object> summary = ordersMapper.aggregateStats(start.atStartOfDay(), end.atStartOfDay());
        Map<String, Object> data = new LinkedHashMap<>(summary == null ? Map.of() : summary);
        Map<String, Object> today = ordersMapper.aggregateStats(LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay());
        long periodDays = Math.max(1, ChronoUnit.DAYS.between(start, end));
        LocalDate previousStart = start.minusDays(periodDays);
        Map<String, Object> previous = ordersMapper.aggregateStats(previousStart.atStartOfDay(), start.atStartOfDay());
        data.put("today", today == null ? Map.of() : today);
        data.put("averageOrderValue", averageOrderValue(summary));
        data.put("trend", ordersMapper.dailyRevenueStats(start.atStartOfDay(), end.atStartOfDay()));
        data.put("growth", growth(start, end, previousStart, summary, previous));
        return HttpResult.success(data);
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

    private BigDecimal averageOrderValue(Map<String, Object> data) {
        if (data == null) return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        Number completed = (Number) data.get("completedCount");
        Number revenue = (Number) data.get("revenue");
        if (completed == null || completed.longValue() == 0 || revenue == null) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(revenue.doubleValue())
                .divide(BigDecimal.valueOf(completed.longValue()), 1, RoundingMode.HALF_UP);
    }

    private Map<String, Object> growth(LocalDate start, LocalDate end, LocalDate previousStart,
            Map<String, Object> current, Map<String, Object> previous) {
        int users = userMapper.countCreatedBetween(start.atStartOfDay(), end.atStartOfDay());
        int previousUsers = userMapper.countCreatedBetween(previousStart.atStartOfDay(), start.atStartOfDay());
        int businesses = businessMapper.countCreatedBetween(start.atStartOfDay(), end.atStartOfDay());
        int previousBusinesses = businessMapper.countCreatedBetween(previousStart.atStartOfDay(), start.atStartOfDay());
        Number previousRevenue = previous == null ? null : (Number) previous.get("revenue");
        Number currentRevenue = current == null ? null : (Number) current.get("revenue");
        BigDecimal revenueRate = null;
        if (previousRevenue != null && previousRevenue.doubleValue() > 0 && currentRevenue != null) {
            revenueRate = BigDecimal.valueOf((currentRevenue.doubleValue() - previousRevenue.doubleValue())
                    / previousRevenue.doubleValue() * 100).setScale(1, RoundingMode.HALF_UP);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", users - previousUsers);
        result.put("businesses", businesses - previousBusinesses);
        result.put("revenueRate", revenueRate);
        return result;
    }
}
