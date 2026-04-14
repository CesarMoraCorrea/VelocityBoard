package com.example.VelocityBoard.dto;

import com.example.VelocityBoard.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de edición de usuario.
 * Todos los campos son opcionales: solo se actualizan los que no sean null.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para actualizar un usuario. Solo se modifican los campos enviados (no nulos).")
public class UpdateUserRequest {

    @Schema(description = "Nuevo nombre de usuario", example = "nuevo_username")
    private String username;

    @Schema(description = "Nuevo correo electrónico", example = "nuevo@email.com")
    private String email;

    @Schema(description = "Nueva contraseña (se almacena encriptada)", example = "nuevaPassword123")
    private String password;

    @Schema(description = "Nuevo rol del usuario", example = "ROLE_ADMIN", allowableValues = {"ROLE_USER", "ROLE_ADMIN"})
    private Role role;
}
