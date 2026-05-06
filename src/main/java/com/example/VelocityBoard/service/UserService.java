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

@Service
@RequiredArgsConstructor
public class UserService implements ListUsersUseCase, UpdateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

    public Mono<User> registerUser(User user) {
        return userRepository.existsByUsername(user.getUsername())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("Username already exists"));
                    }
                    return userRepository.existsByEmail(user.getEmail());
                })
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("Email already exists"));
                    }
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    return userRepository.save(user);
                });
    }
}
