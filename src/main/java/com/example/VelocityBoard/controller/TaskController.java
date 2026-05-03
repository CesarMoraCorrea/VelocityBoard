package com.example.VelocityBoard.controller;

import com.example.VelocityBoard.model.Task;
import com.example.VelocityBoard.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Task> saveTask(@RequestBody Task task) {
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(new java.util.Date());
        }
        return taskService.saveAndEmitTask(task);
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Task> getEvents() {
        // Devuelve un Flux<Task> manejado de forma reactiva (Non-Blocking)
        return taskService.getTaskEvents();
    }

    @PutMapping("/{id}")
    public Mono<Task> updateTask(@PathVariable String id, @RequestBody Task task) {
        return taskService.updateTask(id, task.getTitle(), task.getDescription());
    }

    @DeleteMapping("/{id}")
    public Mono<Task> softDeleteTask(@PathVariable String id) {
        return taskService.softDeleteTask(id);
    }

    @PutMapping("/{id}/restore")
    public Mono<Task> restoreTask(@PathVariable String id) {
        return taskService.restoreTask(id);
    }

    @PutMapping("/{id}/archive")
    public Mono<Task> archiveTask(@PathVariable String id) {
        return taskService.archiveTask(id);
    }

    @PutMapping("/{id}/unarchive")
    public Mono<Task> unarchiveTask(@PathVariable String id) {
        return taskService.unarchiveTask(id);
    }

    @DeleteMapping("/{id}/hard")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> hardDeleteTask(@PathVariable String id) {
        return taskService.hardDeleteTask(id);
    }
}
