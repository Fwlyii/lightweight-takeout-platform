package com.tju.elm_bk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Push is optional and runs only after commit; transport failure cannot undo an approval. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {
    private final ObjectProvider<NotificationTransport> transports;

    public void sendToClient(String userId, String message) {
        afterCommit(() -> deliver(transport -> transport.sendToClient(userId, message)));
    }

    public void sendToAuthority(String authority, String message) {
        afterCommit(() -> deliver(transport -> transport.sendToAuthority(authority, message)));
    }

    private void deliver(java.util.function.Consumer<NotificationTransport> delivery) {
        transports.orderedStream().forEach(transport -> {
            try {
                delivery.accept(transport);
            } catch (RuntimeException ex) {
                log.warn("Real-time notification failed; persistent records are unaffected", ex);
            }
        });
    }

    private void afterCommit(Runnable delivery) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { delivery.run(); }
            });
        } else {
            delivery.run();
        }
    }
}
