package com.example.VelocityBoard.dto;

import com.example.VelocityBoard.model.Role;
import com.example.VelocityBoard.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para exponer datos de usuario sin incluir la contraseña.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;
    private String username;
    private String email;
    private Role role;

    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
