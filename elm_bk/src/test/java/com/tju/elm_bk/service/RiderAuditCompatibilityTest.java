package com.tju.elm_bk.service;

import com.tju.elm_bk.dto.RiderAuditDTO;
import com.tju.elm_bk.entity.Authority;
import com.tju.elm_bk.entity.RiderProfile;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.*;
import com.tju.elm_bk.service.impl.RiderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.tju.elm_bk.entity.Notification;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RiderAuditCompatibilityTest {
    private final RiderMapper riders = mock(RiderMapper.class);
    private final AuthorityMapper authorities = mock(AuthorityMapper.class);
    private final UserAuthorityMapper grants = mock(UserAuthorityMapper.class);
    private final NotificationMapper records = mock(NotificationMapper.class);
    private final NotificationDispatcher push = mock(NotificationDispatcher.class);
    private final RiderServiceImpl service = new RiderServiceImpl(riders, authorities, grants,
            mock(DeliveryTaskMapper.class), records, push, mock(CurrentUserService.class));
    private final RiderAuditDTO dto = new RiderAuditDTO();
    private RiderProfile pending;

    @BeforeEach
    void setup() {
        pending = new RiderProfile();
        pending.setId(1L);
        pending.setUserId(12L);
        pending.setAuditStatus(0);
        dto.setApproved(true);
        when(riders.findById(1L)).thenReturn(pending);
        when(riders.audit(1L, 1, null)).thenReturn(1);
        when(authorities.findByName("RIDER")).thenReturn(new Authority());
    }

    @Test
    void approvalGrantsRiderAndPersistsNotification() {
        service.audit(1L, dto);
        verify(grants).insertUserAuthority(12L, "RIDER");
        var capture = ArgumentCaptor.forClass(Notification.class);
        verify(records).insert(capture.capture());
        assertEquals(12L, capture.getValue().getUserId());
        assertEquals(1, capture.getValue().getAuditResult());
        verify(push).sendToClient(eq("12"), anyString());
    }

    @Test
    void alreadyReviewedApplicationCannotGrantAgain() {
        pending.setAuditStatus(1);
        assertThrows(APIException.class, () -> service.audit(1L, dto));
        verifyNoInteractions(grants, records, push);
        verify(riders, never()).audit(anyLong(), anyInt(), any());
    }

    @Test
    void concurrentAuditLoserCannotGrantOrNotify() {
        when(riders.audit(1L, 1, null)).thenReturn(0);
        assertThrows(APIException.class, () -> service.audit(1L, dto));
        verifyNoInteractions(grants, records, push);
    }

    @Test
    void rejectionNeedsReasonAndDoesNotGrantRole() {
        dto.setApproved(false);
        assertThrows(APIException.class, () -> service.audit(1L, dto));
        verifyNoInteractions(grants, records, push);
        dto.setReason(" 材料不全 ");
        when(riders.audit(1L, 2, "材料不全")).thenReturn(1);
        service.audit(1L, dto);
        verify(riders).audit(1L, 2, "材料不全");
        verifyNoInteractions(grants);
        verify(records).insert(any());
    }

    @Test
    void missingConfigurationDoesNotSendSuccessNotification() {
        when(authorities.findByName("RIDER")).thenReturn(null);
        assertThrows(APIException.class, () -> service.audit(1L, dto));
        verifyNoInteractions(grants, records, push);
    }

    @Test
    void invalidInputFailsBeforeDatabaseAccess() {
        assertThrows(APIException.class, () -> service.audit(1L, null));
        assertThrows(APIException.class, () -> service.audit(-1L, dto));
        assertThrows(APIException.class, () -> service.listApplications(99));
        verifyNoInteractions(riders);
    }
}
