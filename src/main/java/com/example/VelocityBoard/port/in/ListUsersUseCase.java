package com.example.VelocityBoard.port.in;

import com.example.VelocityBoard.dto.UserResponse;
import reactor.core.publisher.Flux;

/**
 * Puerto de entrada (input port) — Arquitectura Hexagonal.
 * Define el caso de uso para listar todos los usuarios del sistema.
 */
public interface ListUsersUseCase {
    Flux<UserResponse> listAllUsers();
}
