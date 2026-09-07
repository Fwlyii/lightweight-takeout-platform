package com.tju.elm_bk.service;

import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Resolves identity from the validated security context, never from a request userId. */
@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserMapper users;

    public Optional<User> optionalUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) return Optional.empty();
        return Optional.ofNullable(users.findByUsernameWithAuthorities(authentication.getName()))
                .filter(account -> Boolean.TRUE.equals(account.getActivated()) && !Boolean.TRUE.equals(account.getIsDeleted()));
    }

    public User requireUser() {
        return optionalUser().orElseThrow(() -> new InsufficientAuthenticationException("请先登录"));
    }

    public Long requireUserId() { return requireUser().getId(); }
}
