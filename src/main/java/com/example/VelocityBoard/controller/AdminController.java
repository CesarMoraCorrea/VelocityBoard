package com.example.VelocityBoard.controller;

import com.example.VelocityBoard.dto.UserResponse;
import com.example.VelocityBoard.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Flux<UserResponse> listAllUsers() {
        return userService.listAllUsers();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable String id) {
        return userService.deleteUser(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(IllegalArgumentException.class, e -> 
                    Mono.just(ResponseEntity.notFound().build()));
    }
}
