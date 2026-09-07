package com.tju.elm_bk.service;

import com.tju.elm_bk.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Serializes per-account writes across requests/instances. Caller must own a transaction. */
@Service
@RequiredArgsConstructor
public class AccountWriteLock {
    private final UserMapper users;
    private final CurrentUserService currentUser;

    @Transactional(propagation = Propagation.MANDATORY)
    public Long acquire() {
        Long id = currentUser.requireUserId();
        users.lockAccount(id);
        currentUser.requireUser();
        return id;
    }
}
