package com.driply.payments.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchangeSpec -> exchangeSpec
                        .pathMatchers("/**").permitAll()
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable);
        return http.build();
    }
}
