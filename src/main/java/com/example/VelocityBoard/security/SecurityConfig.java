package com.example.VelocityBoard.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec
                        .authenticationEntryPoint((swe, e) -> {
                            swe.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            swe.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            byte[] bytes = "{\"error\":\"No autenticado\",\"message\":\"Debes enviar un token JWT válido en el header Authorization: Bearer <token>\"}".getBytes();
                            DataBuffer buffer = swe.getResponse().bufferFactory().wrap(bytes);
                            return swe.getResponse().writeWith(Mono.just(buffer));
                        })
                        .accessDeniedHandler((swe, e) -> {
                            swe.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            swe.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            byte[] bytes = "{\"error\":\"Acceso denegado\",\"message\":\"No tienes permisos para acceder a este recurso. Se requiere rol ROLE_ADMIN.\"}".getBytes();
                            DataBuffer buffer = swe.getResponse().bufferFactory().wrap(bytes);
                            return swe.getResponse().writeWith(Mono.just(buffer));
                        })
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authenticationManager(authenticationManager)
                .securityContextRepository(securityContextRepository)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/auth/**", "/index.html", "/activate.html", "/", "/swagger.html").permitAll()
                        .pathMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll()
                        .anyExchange().authenticated()
                )
                .build();
    }
}
