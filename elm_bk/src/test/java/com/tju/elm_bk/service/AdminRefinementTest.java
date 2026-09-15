package com.tju.elm_bk.service;

import com.tju.elm_bk.controller.AdminController;
import com.tju.elm_bk.controller.AdminRiderController;
import com.tju.elm_bk.mapper.*;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AdminRefinementTest {
    @Test void trendGroupingUsesTheSelectedDayExpressionForStrictMysql() throws Exception {
        String sql=OrdersMapper.class.getMethod("dailyRevenueStats",LocalDateTime.class,LocalDateTime.class).getAnnotation(Select.class).value()[0];
        assertTrue(sql.contains("GROUP BY DATE_FORMAT(order_date, '%Y-%m-%d')"));
    }
    @Test void dashboardEnrichesExistingTotalsWithoutChangingTheirContract() {
        var orders=mock(OrdersMapper.class);var users=mock(UserMapper.class);var businesses=mock(BusinessMapper.class);
        when(orders.aggregateStats(any(),any())).thenReturn(Map.of("completedCount",2,"revenue",new BigDecimal("25.00")));
        when(orders.dailyRevenueStats(any(),any())).thenReturn(List.of(Map.of("day","2026-09-15","revenue",25)));
        var response=new AdminController(users,businesses,orders).statistics(LocalDate.of(2026,9,1),LocalDate.of(2026,9,15));
        assertEquals(new BigDecimal("12.5"),response.getData().get("averageOrderValue"));
        assertEquals(2,response.getData().get("completedCount"));
        assertNotNull(response.getData().get("trend"));assertNotNull(response.getData().get("today"));
    }
    @Test void newDispatchSummaryRemainsAdminOnly() {
        assertEquals("hasAuthority('ADMIN')",AdminRiderController.class.getAnnotation(PreAuthorize.class).value());
    }
}
