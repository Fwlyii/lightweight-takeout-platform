package com.tju.elm_bk.service;

import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service("userDetailsService")
@RequiredArgsConstructor
public class UserModelDetailsService implements UserDetailsService {
    private final UserMapper users;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) {
        String identifier = login == null ? "" : login.trim();
        User account = users.findByUsernameWithAuthorities(identifier);
        if (account == null && identifier.matches("^1[3-9]\\d{9}$")) {
            account = users.findByPhoneWithAuthorities(identifier);
        }
        if (account == null || account.getAuthorities() == null) {
            throw new UsernameNotFoundException("账号或密码错误");
        }
        var authorities = account.getAuthorities().stream().filter(Objects::nonNull)
                .map(a -> a.getName()).filter(n -> n != null && !n.isBlank())
                .map(SimpleGrantedAuthority::new).toList();
        if (authorities.isEmpty()) throw new UsernameNotFoundException("账号或密码错误");
        return org.springframework.security.core.userdetails.User.withUsername(account.getUsername())
                .password(account.getPassword()).authorities(authorities)
                .disabled(!Boolean.TRUE.equals(account.getActivated())).build();
    }
}
