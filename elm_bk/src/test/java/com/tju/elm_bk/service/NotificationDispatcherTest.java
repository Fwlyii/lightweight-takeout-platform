package com.tju.elm_bk.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class NotificationDispatcherTest {
    private final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
    private final NotificationTransport transport = mock(NotificationTransport.class);
    private NotificationDispatcher dispatcher() {
        beans.registerSingleton("transport", transport);
        return new NotificationDispatcher(beans.getBeanProvider(NotificationTransport.class));
    }

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void missingTransportDoesNotBreakBusinessOperation() {
        var dispatcher = new NotificationDispatcher(beans.getBeanProvider(NotificationTransport.class));
        assertDoesNotThrow(() -> dispatcher.sendToClient("12", "approved"));
    }

    @Test
    void sendsOutsideTransaction() {
        dispatcher().sendToAuthority("ADMIN", "new application");
        verify(transport).sendToAuthority("ADMIN", "new application");
    }

    @Test
    void sendsOnlyAfterTransactionCommits() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        dispatcher().sendToClient("12", "approved");
        verifyNoInteractions(transport);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(transport).sendToClient("12", "approved");
    }

    @Test
    void rollbackDoesNotPushUncommittedResult() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        dispatcher().sendToClient("12", "approved");
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(transport);
    }

    @Test
    void transportFailureDoesNotTurnCommittedApprovalIntoHttpFailure() {
        doThrow(new IllegalStateException("offline")).when(transport).sendToClient("12", "approved");
        assertDoesNotThrow(() -> dispatcher().sendToClient("12", "approved"));
    }
}
