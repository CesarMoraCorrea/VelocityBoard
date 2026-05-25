package com.example.VelocityBoard.config;

import com.example.VelocityBoard.model.Tablero;
import com.example.VelocityBoard.model.Task;
import com.example.VelocityBoard.model.User;
import com.example.VelocityBoard.repository.TableroRepository;
import com.example.VelocityBoard.repository.TaskRepository;
import com.example.VelocityBoard.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TableroRepository tableroRepository;

    public DatabaseMigrationRunner(UserRepository userRepository, TaskRepository taskRepository, TableroRepository tableroRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.tableroRepository = tableroRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Starting database consistency migration for ghost users...");

        userRepository.findAll()
                .map(User::getId)
                .collectList()
                .flatMap(existingUserIds -> {
                    Set<String> userIdsSet = new HashSet<>(existingUserIds);
                    log.info("Found {} active users in the database.", userIdsSet.size());

                    // 1. Sanitize tasks
                    Mono<Long> tasksMigration = taskRepository.findAll()
                            .filter(task -> task.getUserId() != null && !userIdsSet.contains(task.getUserId()))
                            .flatMap(task -> {
                                log.info("Task '{}' (#{}) has a ghost user ID '{}'. Setting to null (Unassigned).",
                                        task.getTitle(), task.getId(), task.getUserId());
                                task.setUserId(null);
                                task.setAssignedUserEmail(null);
                                return taskRepository.save(task);
                            })
                            .count()
                            .doOnSuccess(count -> log.info("Sanitized {} tasks with deleted/ghost users.", count));

                    // 2. Sanitize tableros
                    Mono<Long> tablerosMigration = tableroRepository.findAll()
                            .flatMap(tablero -> {
                                List<String> originalMiembros = tablero.getMiembros();
                                if (originalMiembros == null) {
                                    return Mono.empty();
                                }
                                List<String> sanitizedMiembros = originalMiembros.stream()
                                        .filter(userIdsSet::contains)
                                        .collect(Collectors.toList());

                                if (sanitizedMiembros.size() != originalMiembros.size()) {
                                    log.info("Board '{}' (#{}) has ghost members. Removing them. Original count: {}, Sanitized count: {}",
                                            tablero.getNombre(), tablero.getId(), originalMiembros.size(), sanitizedMiembros.size());
                                    tablero.setMiembros(sanitizedMiembros);
                                    return tableroRepository.save(tablero);
                                }
                                return Mono.empty();
                            })
                            .count()
                            .doOnSuccess(count -> log.info("Sanitized {} tableros with ghost members.", count));

                    return Mono.zip(tasksMigration, tablerosMigration);
                })
                .subscribe(
                        result -> log.info("Database migration completed successfully. Tasks cleaned: {}, Boards cleaned: {}", result.getT1(), result.getT2()),
                        error -> log.error("Database migration failed: {}", error.getMessage(), error)
                );
    }
}
