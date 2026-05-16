package com.example.VelocityBoard.service;

import com.example.VelocityBoard.dto.UpdateUserRequest;
import com.example.VelocityBoard.dto.UserResponse;
import com.example.VelocityBoard.model.User;
import com.example.VelocityBoard.port.in.ListUsersUseCase;
import com.example.VelocityBoard.port.in.UpdateUserUseCase;
import com.example.VelocityBoard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import com.example.VelocityBoard.model.VerificationToken;
import com.example.VelocityBoard.repository.VerificationTokenRepository;
@Service
@RequiredArgsConstructor
public class UserService implements ListUsersUseCase, UpdateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final com.example.VelocityBoard.repository.TableroRepository tableroRepository;
    private final com.example.VelocityBoard.repository.TaskRepository taskRepository;

    public Mono<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Flux<UserResponse> listAllUsers() {
        return userRepository.findAll()
                .map(UserResponse::fromUser);
    }

    @Override
    public Flux<UserResponse> searchUsers(String query) {
        return userRepository.searchUsers(query)
                .map(UserResponse::fromUser);
    }

    @Override
    public Flux<UserResponse> getUsersByIds(List<String> ids) {
        return userRepository.findAllById(ids)
                .map(UserResponse::fromUser);
    }

    @Override
    public Mono<UserResponse> updateUser(String id, UpdateUserRequest request) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Usuario no encontrado con id: " + id)))
                .flatMap(user -> {
                    // Validar username único solo si cambia
                    boolean usernameChanges = request.getUsername() != null
                            && !request.getUsername().isBlank()
                            && !request.getUsername().equals(user.getUsername());
                    boolean emailChanges = request.getEmail() != null
                            && !request.getEmail().isBlank()
                            && !request.getEmail().equals(user.getEmail());

                    Mono<User> validation = Mono.just(user);

                    if (usernameChanges) {
                        validation = validation.flatMap(u ->
                                userRepository.existsByUsername(request.getUsername())
                                        .flatMap(exists -> exists
                                                ? Mono.error(new IllegalArgumentException("El username ya está en uso"))
                                                : Mono.just(u)));
                    }
                    if (emailChanges) {
                        validation = validation.flatMap(u ->
                                userRepository.existsByEmail(request.getEmail())
                                        .flatMap(exists -> exists
                                                ? Mono.error(new IllegalArgumentException("El email ya está en uso"))
                                                : Mono.just(u)));
                    }

                    return validation.flatMap(u -> {
                        if (usernameChanges) u.setUsername(request.getUsername());
                        if (emailChanges)    u.setEmail(request.getEmail());
                        if (request.getPassword() != null && !request.getPassword().isBlank()) {
                            u.setPassword(passwordEncoder.encode(request.getPassword()));
                        }
                        if (request.getRole() != null) u.setRole(request.getRole());
                        return userRepository.save(u);
                    });
                })
                .map(UserResponse::fromUser);
    }

    @org.springframework.beans.factory.annotation.Value("${APP_URL:http://localhost:8081}")
    private String appUrl;

    public Mono<User> registerUser(User user) {
        if ("JD60721".equalsIgnoreCase(user.getUsername())) {
            user.setRole(com.example.VelocityBoard.model.Role.ROLE_ADMIN);
        }

        return userRepository.findByUsername(user.getUsername())
                .flatMap(existing -> {
                    if (existing.isActive()) {
                        return Mono.error(new IllegalArgumentException("Username already exists"));
                    }
                    return tokenRepository.deleteByUserId(existing.getId())
                            .then(userRepository.delete(existing));
                })
                .then(userRepository.findByEmail(user.getEmail()))
                .flatMap(existing -> {
                    if (existing.isActive()) {
                        return Mono.error(new IllegalArgumentException("Email already exists"));
                    }
                    return tokenRepository.deleteByUserId(existing.getId())
                            .then(userRepository.delete(existing));
                })
                .then(Mono.defer(() -> {
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    user.setActive(false); // Make sure it's inactive
                    return userRepository.save(user);
                }))
                .flatMap(savedUser -> {
                    String tokenStr = UUID.randomUUID().toString();
                    VerificationToken token = VerificationToken.builder()
                            .token(tokenStr)
                            .userId(savedUser.getId())
                            .expiryDate(LocalDateTime.now().plusMinutes(30))
                            .build();
                            
                    String activationLink = appUrl + "/activate.html?token=" + tokenStr;
                    String emailHtml = "<h1>Bienvenido a VelocityBoard</h1>"
                            + "<p>Por favor, haz clic en el siguiente enlace para activar tu cuenta:</p>"
                            + "<a href=\"" + activationLink + "\">Activar Cuenta</a>";

                    return tokenRepository.save(token)
                            .flatMap(t -> emailService.sendHtmlEmail(savedUser.getEmail(), "Activa tu cuenta", emailHtml)
                                    .onErrorResume(e -> {
                                        System.err.println("Email Error: " + e.getMessage());
                                        if (e.getCause() != null) {
                                            System.err.println("Cause: " + e.getCause().getMessage());
                                        }
                                        e.printStackTrace();
                                        return tokenRepository.delete(t)
                                                .then(userRepository.delete(savedUser))
                                                .then(Mono.error(new IllegalArgumentException("Error enviando el correo: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()))));
                                    })
                            )
                            .thenReturn(savedUser);
                });
    }

    public Mono<Void> activateUser(String token) {
        return tokenRepository.findByToken(token)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid token")))
                .flatMap(verificationToken -> {
                    if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                        return tokenRepository.delete(verificationToken)
                                .then(Mono.error(new IllegalArgumentException("Expired token")));
                    }
                    return userRepository.findById(verificationToken.getUserId())
                            .flatMap(user -> {
                                user.setActive(true);
                                return userRepository.save(user);
                            })
                            .then(tokenRepository.delete(verificationToken));
                });
    }

    public Mono<Void> deleteUser(String id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Usuario no encontrado con id: " + id)))
                .flatMap(user -> Mono.when(
                        taskRepository.deleteByUserId(id),
                        tableroRepository.deleteByPropietarioId(id),
                        tokenRepository.deleteByUserId(id)
                ).then(userRepository.delete(user)));
    }
}
