package com.example.VelocityBoard.controller;

import com.example.VelocityBoard.dto.CollaboratorResponse;
import com.example.VelocityBoard.security.JwtUtil;
import com.example.VelocityBoard.service.TableroService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardsApiController {

    private final TableroService tableroService;
    private final JwtUtil jwtUtil;

    @GetMapping("/{id}/collaborators")
    public Flux<CollaboratorResponse> obtenerColaboradores(@PathVariable String id) {
        return obtenerUserId()
                .flatMapMany(userId -> tableroService.listarColaboradores(id, userId));
    }

    private Mono<String> obtenerUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> {
                    String token = auth.getCredentials().toString();
                    return jwtUtil.getAllClaimsFromToken(token).get("userId", String.class);
                });
    }
}
