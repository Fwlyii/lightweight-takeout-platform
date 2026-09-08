package com.tju.elm_bk.controller;

import com.tju.elm_bk.mapper.NotificationMapper;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationControllerTest {
    private final NotificationMapper records = mock(NotificationMapper.class);
    private final CurrentUserService identity = mock(CurrentUserService.class);
    private final NotificationController controller = new NotificationController(
            new NotificationServiceImpl(records), identity);

    @Test
    void historyIgnoresCallerSuppliedOwnerAndNeedsNoRealtimeTransport() {
        when(identity.requireUserId()).thenReturn(7L);
        controller.getNotifications(99L);
        verify(records).list(7L);
        verify(records, never()).list(99L);
    }

    @Test
    void readUpdateAlwaysIncludesAuthenticatedOwner() {
        when(identity.requireUserId()).thenReturn(7L);
        controller.readNotification(12L);
        verify(records).updateRead(eq(12L), eq(7L), any());
    }

    @Test
    void missingIdentityCannotQueryOrUpdateNotifications() {
        when(identity.requireUserId()).thenThrow(new InsufficientAuthenticationException("请先登录"));
        assertThrows(InsufficientAuthenticationException.class, () -> controller.getNotifications(7L));
        assertThrows(InsufficientAuthenticationException.class, () -> controller.readNotification(12L));
        verifyNoInteractions(records);
    }
}
