package com.tju.elm_bk.security;

import com.tju.elm_bk.entity.Authority;
import com.tju.elm_bk.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class TokenSessionTest {
    private static final String KEY = "authentication-tests-only-0123456789abcdef0123456789abcdef0123456789";
    private final TokenProvider tokens = new TokenProvider(KEY, 3600, 7200);

    @Test
    void signedSessionCanBeCheckedWithoutRetainingCredentials() {
        var session = tokens.readSession(validToken());
        assertThat(session.isCurrentFor(account())).isTrue();
        assertThat(session.authentication().getCredentials()).isNull();
        assertThat(session.authentication().getAuthorities()).extracting("authority").containsExactly("USER");
    }

    @Test
    void signedSessionCannotBeAppliedToAnotherAccount() {
        var other = account();
        other.setUsername("someone_else");
        assertThat(tokens.readSession(validToken()).isCurrentFor(other)).isFalse();
    }

    @Test
    void signedButNonExpiringTokenIsRejected() {
        var token = Jwts.builder().setSubject("student").claim("auth", "USER").claim("session_role", "user")
                .claim("iat_ms", System.currentTimeMillis())
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8))).compact();
        assertThat(tokens.validateToken(token)).isFalse();
    }

    @Test
    void signedMultiAuthorityTokenIsRejected() {
        var token = Jwts.builder().setSubject("student").claim("auth", "USER,ADMIN").claim("session_role", "user")
                .claim("iat_ms", System.currentTimeMillis()).setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8))).compact();
        assertThat(tokens.validateToken(token)).isFalse();
    }

    @Test
    void malformedTokenNeverPassesLegacyCompatibilityMethods() {
        assertThat(tokens.validateToken("malformed")).isFalse();
        assertThat(tokens.isCurrentForAccount("malformed", null)).isFalse();
        assertThat(tokens.isRoleBoundAndCurrentForAccount("malformed", account())).isFalse();
    }

    @Test
    void revokedRoleInvalidatesBothSessionAndCompatibilityMethod() {
        String token = validToken();
        var changed = account();
        var rider = new Authority();
        rider.setName("RIDER");
        changed.setAuthorities(List.of(rider));
        assertThat(tokens.readSession(token).isCurrentFor(changed)).isFalse();
        assertThat(tokens.isRoleBoundAndCurrentForAccount(token, changed)).isFalse();
    }

    private String validToken() {
        return tokens.createRoleBoundToken(UsernamePasswordAuthenticationToken.authenticated(
                "student", "not-kept", List.of(new SimpleGrantedAuthority("USER"))), false, "user");
    }

    @Test
    void browserWebSocketHandshakeUsesTheSameSignedSessionValidation() throws Exception {
        assertThat(filterQueryToken("/ws/7", true, validToken(), account())).isNotNull();
    }

    @Test
    void queryTokenDoesNotAuthenticateOrdinaryHttpRequests() throws Exception {
        assertThat(filterQueryToken("/api/user", true, validToken(), account())).isNull();
        assertThat(filterQueryToken("/ws/7", false, validToken(), account())).isNull();
    }

    @Test
    void invalidOrRevokedQueryTokenCannotAuthenticateWebSocket() throws Exception {
        assertThat(filterQueryToken("/ws/7", true, "invalid", account())).isNull();
        User disabled = account();
        disabled.setActivated(false);
        assertThat(filterQueryToken("/ws/7", true, validToken(), disabled)).isNull();
    }

    private org.springframework.security.core.Authentication filterQueryToken(
            String path, boolean upgrade, String token, User user) throws Exception {
        var users = org.mockito.Mockito.mock(com.tju.elm_bk.mapper.UserMapper.class);
        org.mockito.Mockito.when(users.findByUsernameWithAuthorities("student")).thenReturn(user);
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        request.addParameter("access_token", token);
        if (upgrade) request.addHeader("Upgrade", "websocket");
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        try {
            new JWTFilter(tokens, users).doFilter(request,
                    new org.springframework.mock.web.MockHttpServletResponse(),
                    new org.springframework.mock.web.MockFilterChain());
            return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    private User account() {
        var account = new User();
        account.setUsername("student");
        account.setActivated(true);
        account.setIsDeleted(false);
        account.setUpdateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        var authority = new Authority();
        authority.setName("USER");
        account.setAuthorities(List.of(authority));
        return account;
    }
}
