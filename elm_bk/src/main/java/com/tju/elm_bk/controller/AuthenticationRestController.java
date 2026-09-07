package com.tju.elm_bk.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tju.elm_bk.dto.AuthenticationDTO;
import com.tju.elm_bk.security.TokenProvider;
import com.tju.elm_bk.service.LoginRolePolicy;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthenticationRestController {
    private final AuthenticationManager authenticationManager;
    private final LoginRolePolicy loginRolePolicy;
    private final TokenProvider tokenProvider;

    @PostMapping("/auth")
    public ResponseEntity<JWTToken> authorize(@Valid @RequestBody AuthenticationDTO request) {
        var account = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername(), request.getPassword()));
        var decision = loginRolePolicy.decide(account, request.getRole());
        var session = UsernamePasswordAuthenticationToken.authenticated(account.getPrincipal(), null,
                List.of(new SimpleGrantedAuthority(decision.sessionAuthority())));
        String token = tokenProvider.createRoleBoundToken(session,
                Boolean.TRUE.equals(request.getRememberMe()), decision.portalKey());
        return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(new JWTToken(token, decision.portalKey(), decision.applicationOnly()));
    }

    public record JWTToken(@JsonProperty("id_token") String idToken, String role,
                           @JsonProperty("application_only") boolean applicationOnly) {}
}
