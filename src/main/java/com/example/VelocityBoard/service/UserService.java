package com.example.VelocityBoard.service;

import com.example.VelocityBoard.dto.UserResponse;
import com.example.VelocityBoard.model.User;
import com.example.VelocityBoard.port.in.ListUsersUseCase;
import com.example.VelocityBoard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService implements ListUsersUseCase {

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
