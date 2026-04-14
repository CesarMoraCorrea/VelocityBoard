package com.example.VelocityBoard.controller;

import com.example.VelocityBoard.dto.AuthRequest;
import com.example.VelocityBoard.dto.AuthResponse;
import com.example.VelocityBoard.dto.RegisterRequest;
import com.example.VelocityBoard.model.User;
import com.example.VelocityBoard.security.JwtUtil;
import com.example.VelocityBoard.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody AuthRequest request) {
        return userService.findByUsername(request.getUsername())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(user -> ResponseEntity.ok(new AuthResponse(jwtUtil.generateToken(user))))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<?>> register(@RequestBody RegisterRequest request) {
        User.UserBuilder builder = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword());
        if (request.getRole() != null) {
            builder.role(request.getRole());
        }
        User user = builder.build();

        return userService.registerUser(user)
                .<ResponseEntity<?>>map(savedUser -> ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(jwtUtil.generateToken(savedUser))))
                .onErrorResume(IllegalArgumentException.class, e -> 
                        Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }
}
