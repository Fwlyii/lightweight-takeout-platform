package com.tju.elm_bk.security;

import com.tju.elm_bk.mapper.UserMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {
    private final TokenProvider tokens;
    private final UserMapper users;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        // Browser WebSocket handshakes cannot set an Authorization header.
        // Query tokens are accepted only for this existing, authenticated transport.
        if (header == null && "GET".equals(request.getMethod())
                && request.getServletPath().startsWith("/ws/")
                && "websocket".equalsIgnoreCase(request.getHeader("Upgrade"))) {
            String[] queryTokens = request.getParameterValues("access_token");
            if (queryTokens != null && queryTokens.length == 1) header = "Bearer " + queryTokens[0];
        }
        if (header != null && header.startsWith("Bearer ")) {
            try {
                var session = tokens.readSession(header.substring(7));
                var account = users.findByUsernameWithAuthorities(session.subject());
                if (session.isCurrentFor(account)) {
                    SecurityContextHolder.getContext().setAuthentication(session.authentication());
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
