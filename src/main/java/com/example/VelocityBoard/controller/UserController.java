package com.example.VelocityBoard.controller;

import com.example.VelocityBoard.dto.UserResponse;
import com.example.VelocityBoard.port.in.ListUsersUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

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
}
