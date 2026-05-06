package com.example.VelocityBoard.controller;

import com.example.VelocityBoard.dto.UpdateUserRequest;
import com.example.VelocityBoard.dto.UserResponse;
import com.example.VelocityBoard.port.in.ListUsersUseCase;
import com.example.VelocityBoard.port.in.UpdateUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Adaptador de entrada (inbound adapter) — Arquitectura Hexagonal.
 * Expone las operaciones de gestión de usuarios a través de HTTP.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestión de usuarios del sistema")
public class UserController {

    private final ListUsersUseCase listUsersUseCase;
    private final UpdateUserUseCase updateUserUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Listar todos los usuarios",
            description = "Retorna la lista completa de usuarios registrados. Requiere rol ADMIN.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de usuarios obtenida exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)))
            ),
            @ApiResponse(responseCode = "401", description = "No autenticado — se requiere token JWT"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    public Flux<UserResponse> listUsers() {
        return listUsersUseCase.listAllUsers();
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar usuarios por nombre o email")
    public Flux<UserResponse> searchUsers(@RequestParam String query) {
        return listUsersUseCase.searchUsers(query);
    }

    @PostMapping("/batch")
    @Operation(summary = "Obtener detalles de usuarios por una lista de IDs")
    public Flux<UserResponse> getUsersByIds(@RequestBody List<String> ids) {
        return listUsersUseCase.getUsersByIds(ids);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Editar un usuario",
            description = "Actualiza los datos de un usuario por su ID. Solo se modifican los campos enviados. Requiere rol ADMIN.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos: usuario no encontrado, username o email ya en uso"),
            @ApiResponse(responseCode = "401", description = "No autenticado — se requiere token JWT"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
    public Mono<ResponseEntity<?>> updateUser(
            @Parameter(description = "ID del usuario a editar") @PathVariable String id,
            @RequestBody UpdateUserRequest request) {
        return updateUserUseCase.updateUser(id, request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(Map.of("error", "Datos inválidos", "message", e.getMessage()))))
                .onErrorResume(Exception.class,
                        e -> Mono.just(ResponseEntity.internalServerError()
                                .body(Map.of("error", "Error interno", "message", e.getMessage()))));
    }
}

