package com.example.VelocityBoard.controller;

import com.example.VelocityBoard.dto.CrearTableroRequest;
import com.example.VelocityBoard.model.Tablero;
import com.example.VelocityBoard.security.JwtUtil;
import com.example.VelocityBoard.service.TableroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;

@RestController
@RequestMapping("/tableros")
@RequiredArgsConstructor
public class TableroController {

    private final TableroService tableroService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Tablero> crearTablero(@Valid @RequestBody CrearTableroRequest request) {
        return obtenerUserId()
                .flatMap(userId -> tableroService.crearTablero(
                        request.getNombre(),
                        request.getDescripcion(),
                        userId));
    }

    @GetMapping
    public Flux<Tablero> listarTableros() {
        return obtenerUserId()
                .flatMapMany(tableroService::listarTablerosPorPropietario);
    }

    @GetMapping("/{id}")
    public Mono<Tablero> obtenerTablero(@PathVariable String id) {
        return obtenerUserId()
                .flatMap(userId -> tableroService.obtenerTableroPorId(id, userId));
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
