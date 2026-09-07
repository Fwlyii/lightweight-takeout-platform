package com.tju.elm_bk.service;

/** Optional real-time adapter. Persistent notifications remain the source of truth. */
public interface NotificationTransport {
    void sendToClient(String userId, String message);
    void sendToAuthority(String authority, String message);
}
