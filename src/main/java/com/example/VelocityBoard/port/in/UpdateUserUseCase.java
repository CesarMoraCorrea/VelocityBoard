package com.example.VelocityBoard.port.in;

import com.example.VelocityBoard.dto.UpdateUserRequest;
import com.example.VelocityBoard.dto.UserResponse;
import reactor.core.publisher.Mono;

/**
 * Puerto de entrada (input port) — Arquitectura Hexagonal.
 * Define el caso de uso para editar los datos de un usuario existente.
 */
public interface UpdateUserUseCase {
    Mono<UserResponse> updateUser(String id, UpdateUserRequest request);
}
