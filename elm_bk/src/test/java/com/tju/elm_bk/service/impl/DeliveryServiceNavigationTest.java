package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.adapter.MapAdapter;
import com.tju.elm_bk.constant.DeliveryTaskStatus;
import com.tju.elm_bk.constant.RiderAuditStatus;
import com.tju.elm_bk.entity.DeliveryTask;
import com.tju.elm_bk.entity.RiderProfile;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.AssetMapper;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.DeliveryTaskMapper;
import com.tju.elm_bk.mapper.FoodMapper;
import com.tju.elm_bk.mapper.NotificationMapper;
import com.tju.elm_bk.mapper.OrderStatusHistoryMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import com.tju.elm_bk.mapper.RiderMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.service.AssetService;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.service.DeliveryNotificationService;
import com.tju.elm_bk.service.OrderSettlementService;
import com.tju.elm_bk.service.OrderStateTransitionService;
import com.tju.elm_bk.vo.DeliveryTaskVO;
import com.tju.elm_bk.websocket.WebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryServiceNavigationTest {
    private DeliveryTaskMapper taskMapper;
    private MapAdapter mapAdapter;
    private DeliveryServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("rider", "", List.of()));
        taskMapper = mock(DeliveryTaskMapper.class);
        RiderMapper riderMapper = mock(RiderMapper.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        mapAdapter = mock(MapAdapter.class);

        User rider = new User();
        rider.setId(7L);
        rider.setUsername("rider");
        RiderProfile profile = new RiderProfile();
        profile.setUserId(7L);
        profile.setAuditStatus(RiderAuditStatus.APPROVED);
        when(currentUserService.requireUser()).thenReturn(rider);
        when(riderMapper.findByUserId(7L)).thenReturn(profile);

        service = new DeliveryServiceImpl(taskMapper, riderMapper, mock(OrdersMapper.class),
                mock(OrderStatusHistoryMapper.class), mock(BusinessMapper.class), currentUserService,
                mock(OrderStateTransitionService.class), mock(OrderSettlementService.class),
                mock(DeliveryNotificationService.class), mapAdapter);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNavigateOwnedAcceptedTaskToMerchant() {
        DeliveryTask task = task(DeliveryTaskStatus.ACCEPTED, 7L);
        DeliveryTaskVO view = new DeliveryTaskVO();
        view.setBusinessName("测试商家");
        view.setBusinessAddress("天津市南开区卫津路92号");
        when(taskMapper.selectById(10L)).thenReturn(task);
        when(taskMapper.selectViewById(10L)).thenReturn(view);
        when(mapAdapter.navigationUrl(view.getBusinessAddress())).thenReturn("https://uri.amap.com/search?keyword=test");
        when(mapAdapter.provider()).thenReturn("AMAP_URI");

        var result = service.getNavigation(10L);

        assertEquals("MERCHANT", result.destinationType());
        assertEquals(view.getBusinessAddress(), result.address());
    }

    @Test
    void shouldRejectTerminalTaskNavigation() {
        when(taskMapper.selectById(10L)).thenReturn(task(DeliveryTaskStatus.COMPLETED, 7L));
        when(taskMapper.selectViewById(10L)).thenReturn(new DeliveryTaskVO());

        assertThrows(APIException.class, () -> service.getNavigation(10L));
        verify(mapAdapter, never()).navigationUrl(org.mockito.ArgumentMatchers.any());
    }

    private DeliveryTask task(DeliveryTaskStatus status, Long riderUserId) {
        DeliveryTask task = new DeliveryTask();
        task.setId(10L);
        task.setOrderId(20L);
        task.setRiderUserId(riderUserId);
        task.setTaskStatus(status.name());
        return task;
    }
}
