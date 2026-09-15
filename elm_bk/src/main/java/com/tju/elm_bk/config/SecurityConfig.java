package com.tju.elm_bk.config;

import com.tju.elm_bk.security.JWTFilter;
import com.tju.elm_bk.security.JwtAccessDeniedHandler;
import com.tju.elm_bk.security.JwtAuthenticationEntryPoint;
import com.tju.elm_bk.service.UserModelDetailsService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserModelDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JWTFilter jwtFilter,
            JwtAuthenticationEntryPoint entryPoint, JwtAccessDeniedHandler denied,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint).accessDeniedHandler(denied))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth", "/api/register", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/images/**", "/uploads/**").permitAll()
                        // Only public catalog reads are anonymous; never expose the whole
                        // businesses/foods namespace (it also contains management actions).
                        .requestMatchers(HttpMethod.GET, "/api/businesses/search",
                                "/api/businesses/type/presentations", "/api/businesses/carousel",
                                "/api/businesses/public/{id:[0-9]+}", "/api/businesses/{id:[0-9]+}/summary", "/api/foods/list",
                                "/api/v1/reviews/public/business/{id:[0-9]+}").permitAll()
                        // Aggregate store counters are also displayed in the merchant workspace;
                        // customer-specific collections and mutations remain USER-only below.
                        .requestMatchers(HttpMethod.GET, "/api/merchant/interaction/stats/{merchantId}")
                                .hasAnyAuthority("USER", "BUSINESS", "ADMIN")
                        .requestMatchers("/api/carts/**", "/api/addresses/**", "/api/merchant/interaction/**", "/api/v1/assets/**",
                                "/api/v1/preferences/**").hasAuthority("USER")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public FilterRegistrationBean<JWTFilter> jwtServletRegistration(JWTFilter jwtFilter) {
        var registration = new FilterRegistrationBean<>(jwtFilter);
        registration.setEnabled(false); // JWT runs in Spring Security's chain, not twice as a servlet filter.
        return registration;
    }
}
