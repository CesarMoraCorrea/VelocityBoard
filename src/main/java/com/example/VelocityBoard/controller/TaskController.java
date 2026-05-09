package com.example.VelocityBoard.controller;

import com.example.VelocityBoard.model.Column;
import com.example.VelocityBoard.model.Task;
import com.example.VelocityBoard.model.TaskActivity;
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

    // Returns array: [userId, username]
    private Mono<String[]> obtenerUserInfo() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> {
                    String token = auth.getCredentials().toString();
                    String userId = jwtUtil.getAllClaimsFromToken(token).get("userId", String.class);
                    String username = jwtUtil.getUsernameFromToken(token);
                    return new String[]{userId, username};
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
        return obtenerUserInfo()
                .flatMap(info -> {
                    String userId = info[0];
                    String username = info[1];
                    task.setCreatedBy(username);
                    task.setUpdatedBy(username);
                    task.setUpdatedAt(new java.util.Date());
                    return checkColumnAccess(task.getColumnId(), userId)
                            .then(taskService.saveAndEmitTask(task, username, "creó la tarea"));
                });
    }

    @GetMapping("/{id}/history")
    public Flux<TaskActivity> getTaskHistory(@PathVariable String id) {
        return obtenerUserInfo()
                .flatMap(info -> checkTaskAccess(id, info[0]).thenReturn(info))
                .thenMany(taskService.getTaskHistory(id)
                        .switchIfEmpty(
                                taskService.getTaskById(id).flatMapMany(task -> {
                                    java.util.List<TaskActivity> fallback = new java.util.ArrayList<>();
                                    if (task.getCreatedBy() != null) {
                                        fallback.add(TaskActivity.builder()
                                                .taskId(task.getId())
                                                .username(task.getCreatedBy())
                                                .action("creó la tarea")
                                                .timestamp(task.getCreatedAt() != null ? task.getCreatedAt() : new java.util.Date())
                                                .build());
                                    }
                                    if (task.getUpdatedBy() != null && task.getUpdatedAt() != null) {
                                        fallback.add(0, TaskActivity.builder()
                                                .taskId(task.getId())
                                                .username(task.getUpdatedBy())
                                                .action("editó la tarea (legacy)")
                                                .timestamp(task.getUpdatedAt())
                                                .build());
                                    }
                                    return Flux.fromIterable(fallback);
                                })
                        )
                )
                .onErrorResume(e -> Flux.empty());
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
        return obtenerUserInfo()
                .flatMap(info -> checkColumnAccess(columnId, info[0]))
                .thenMany(taskService.getTasksByColumnId(columnId));
    }

    @GetMapping("/tablero/{tableroId}/deleted")
    public Flux<Task> getDeletedTasksByTablero(@PathVariable String tableroId) {
        return obtenerUserInfo()
                .flatMap(info -> checkTableroAccess(tableroId, info[0]))
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
        return obtenerUserInfo()
                .flatMap(info -> checkTaskAccess(id, info[0])
                        .then(taskService.updateTask(id, task, info[1])));
    }

    @DeleteMapping("/{id}")
    public Mono<Task> softDeleteTask(@PathVariable String id) {
        return obtenerUserInfo()
                .flatMap(info -> checkTaskAccess(id, info[0])
                        .then(taskService.softDeleteTask(id, info[1])));
    }

    @PutMapping("/{id}/restore")
    public Mono<Task> restoreTask(@PathVariable String id) {
        return obtenerUserInfo()
                .flatMap(info -> checkTaskAccess(id, info[0])
                        .then(taskService.restoreTask(id, info[1])));
    }

    @PutMapping("/{id}/archive")
    public Mono<Task> archiveTask(@PathVariable String id) {
        return obtenerUserInfo()
                .flatMap(info -> checkTaskAccess(id, info[0])
                        .then(taskService.archiveTask(id, info[1])));
    }

    @PutMapping("/{id}/unarchive")
    public Mono<Task> unarchiveTask(@PathVariable String id) {
        return obtenerUserInfo()
                .flatMap(info -> checkTaskAccess(id, info[0])
                        .then(taskService.unarchiveTask(id, info[1])));
    }

    @DeleteMapping("/{id}/hard")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> hardDeleteTask(@PathVariable String id) {
        return obtenerUserInfo()
                .flatMap(info -> checkTaskAccess(id, info[0])
                        .then(taskService.hardDeleteTask(id, info[1])));
    }

    public static class DuplicateRequest {
        public String targetColumnId;
    }

    @PostMapping("/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Task> duplicateTask(@PathVariable String id, @RequestBody(required = false) DuplicateRequest request) {
        String targetColumnId = (request != null) ? request.targetColumnId : null;
        return taskService.duplicateTask(id, targetColumnId);
    }
}
