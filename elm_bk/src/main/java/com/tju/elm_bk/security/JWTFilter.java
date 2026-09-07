package com.tju.elm_bk.security;

import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.service.UserModelDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {
    private final TokenProvider tokens;
    private final UserMapper users;
    private final UserModelDetailsService userDetails;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                if (tokens.validateToken(token)) {
                    var session = tokens.getAuthentication(token);
                    var account = users.findByUsernameWithAuthorities(session.getName());
                    if (account != null && Boolean.TRUE.equals(account.getActivated())
                            && tokens.isCurrentForAccount(token, account.getUpdateTime())
                            && tokens.isRoleBoundAndCurrentForAccount(token, account)) {
                        var principal = userDetails.loadUserByUsername(account.getUsername());
                        SecurityContextHolder.getContext().setAuthentication(
                                UsernamePasswordAuthenticationToken.authenticated(
                                        principal, token, session.getAuthorities()));
                    } else {
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (RuntimeException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
