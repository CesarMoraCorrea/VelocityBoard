package com.example.VelocityBoard.controller;

import com.example.VelocityBoard.model.Column;
import com.example.VelocityBoard.model.Task;
import com.example.VelocityBoard.repository.ColumnRepository;
import com.example.VelocityBoard.repository.TableroRepository;
import com.example.VelocityBoard.security.JwtUtil;
import com.example.VelocityBoard.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;
    private final JwtUtil jwtUtil;
    private final ColumnRepository columnRepository;
    private final TableroRepository tableroRepository;

    public TaskController(TaskService taskService, JwtUtil jwtUtil, ColumnRepository columnRepository, TableroRepository tableroRepository) {
        this.taskService = taskService;
        this.jwtUtil = jwtUtil;
        this.columnRepository = columnRepository;
        this.tableroRepository = tableroRepository;
    }

    private Mono<String> obtenerUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> {
                    String token = auth.getCredentials().toString();
                    return jwtUtil.getAllClaimsFromToken(token).get("userId", String.class);
                });
    }

    private Mono<Void> checkTableroAccess(String tableroId, String userId) {
        return tableroRepository.findById(tableroId)
                .flatMap(t -> {
                    if (t.getPropietarioId().equals(userId) || t.getMiembros().contains(userId)) {
                        return Mono.empty();
                    }
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado al tablero"));
                });
    }

    private Mono<Void> checkColumnAccess(String columnId, String userId) {
        return columnRepository.findById(columnId)
                .flatMap(col -> checkTableroAccess(col.getTableroId(), userId));
    }

    private Mono<Void> checkTaskAccess(String taskId, String userId) {
        return taskService.getTaskById(taskId)
                .flatMap(task -> checkColumnAccess(task.getColumnId(), userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Task> saveTask(@RequestBody Task task) {
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(new java.util.Date());
        }
        return obtenerUserId()
                .flatMap(userId -> checkColumnAccess(task.getColumnId(), userId)
                        .then(taskService.saveAndEmitTask(task)));
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Task> getEvents() {
        return taskService.getTaskEvents();
    }

    @GetMapping("/user/{userId}")
    public Flux<Task> getTasksByUserId(@PathVariable String userId) {
        return taskService.getTasksByUserId(userId);
    }

    @GetMapping("/column/{columnId}")
    public Flux<Task> getTasksByColumnId(@PathVariable String columnId) {
        return obtenerUserId()
                .flatMap(userId -> checkColumnAccess(columnId, userId))
                .thenMany(taskService.getTasksByColumnId(columnId));
    }

    @GetMapping("/tablero/{tableroId}/deleted")
    public Flux<Task> getDeletedTasksByTablero(@PathVariable String tableroId) {
        return obtenerUserId()
                .flatMap(userId -> checkTableroAccess(tableroId, userId))
                .thenMany(columnRepository.findByTableroIdOrderByPositionAsc(tableroId)
                        .map(Column::getId)
                        .collectList()
                        .flatMapMany(columnIds -> {
                            if (columnIds.isEmpty()) return Flux.empty();
                            return taskService.getDeletedTasksByColumnIds(columnIds);
                        })
                );
    }

    @PutMapping("/{id}")
    public Mono<Task> updateTask(@PathVariable String id, @RequestBody Task task) {
        return obtenerUserId()
                .flatMap(userId -> checkTaskAccess(id, userId)
                        .then(taskService.updateTask(id, task)));
    }

    @DeleteMapping("/{id}")
    public Mono<Task> softDeleteTask(@PathVariable String id) {
        return obtenerUserId()
                .flatMap(userId -> checkTaskAccess(id, userId)
                        .then(taskService.softDeleteTask(id)));
    }

    @PutMapping("/{id}/restore")
    public Mono<Task> restoreTask(@PathVariable String id) {
        return obtenerUserId()
                .flatMap(userId -> checkTaskAccess(id, userId)
                        .then(taskService.restoreTask(id)));
    }

    @PutMapping("/{id}/archive")
    public Mono<Task> archiveTask(@PathVariable String id) {
        return obtenerUserId()
                .flatMap(userId -> checkTaskAccess(id, userId)
                        .then(taskService.archiveTask(id)));
    }

    @PutMapping("/{id}/unarchive")
    public Mono<Task> unarchiveTask(@PathVariable String id) {
        return obtenerUserId()
                .flatMap(userId -> checkTaskAccess(id, userId)
                        .then(taskService.unarchiveTask(id)));
    }

    @DeleteMapping("/{id}/hard")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> hardDeleteTask(@PathVariable String id) {
        return obtenerUserId()
                .flatMap(userId -> checkTaskAccess(id, userId)
                        .then(taskService.hardDeleteTask(id)));
    }
}
