package com.example.VelocityBoard.controller;

import com.example.VelocityBoard.security.JwtUtil;
import com.example.VelocityBoard.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final JwtUtil jwtUtil;

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteBoard(@PathVariable String id) {
        return obtenerUserId()
                .flatMap(userId -> boardService.deleteBoard(id, userId));
    }

    public static class RenameBoardRequest {
        public String nombre;
    }

    @PutMapping("/{id}")
    public Mono<com.example.VelocityBoard.model.Tablero> renameBoard(@PathVariable String id, @RequestBody RenameBoardRequest request) {
        return obtenerUserId()
                .flatMap(userId -> boardService.renameBoard(id, request.nombre, userId));
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
