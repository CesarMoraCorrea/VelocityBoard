package com.example.VelocityBoard.service;

import com.example.VelocityBoard.model.Task;
import com.example.VelocityBoard.repository.TaskRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final Sinks.Many<Task> sink;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
        // Use a multicasting sink to broadcast events to all subscribers
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    public Mono<Task> saveAndEmitTask(Task task) {
        return taskRepository.save(task)
                .doOnSuccess(savedTask -> {
                    // Try to emit the event, this broadcasts to all subscribers
                    sink.tryEmitNext(savedTask);
                });
    }

    public Flux<Task> getTaskEvents() {
        return taskRepository.findAll().concatWith(sink.asFlux());
    }

    public Flux<Task> getTasksByUserId(String userId) {
        return taskRepository.findByUserId(userId);
    }

    public Flux<Task> getTasksByColumnId(String columnId) {
        return taskRepository.findByColumnIdOrderByPositionAsc(columnId);
    }

    public Mono<Task> updateTask(String id, Task updatedTask) {
        return taskRepository.findById(id)
                .flatMap(existingTask -> {
                    existingTask.setTitle(updatedTask.getTitle());
                    existingTask.setDescription(updatedTask.getDescription());
                    existingTask.setColumnId(updatedTask.getColumnId());
                    existingTask.setTags(updatedTask.getTags());
                    existingTask.setPosition(updatedTask.getPosition());
                    return taskRepository.save(existingTask)
                        .doOnSuccess(task -> sink.tryEmitNext(task));
                });
    }

    public Mono<Task> softDeleteTask(String id) {
        return taskRepository.findById(id)
                .flatMap(existingTask -> {
                    existingTask.setDeleted(true);
                    return taskRepository.save(existingTask)
                        .doOnSuccess(deletedTask -> sink.tryEmitNext(deletedTask));
                });
    }

    public Mono<Task> restoreTask(String id) {
        return taskRepository.findById(id)
                .flatMap(existingTask -> {
                    existingTask.setDeleted(false);
                    return taskRepository.save(existingTask)
                        .doOnSuccess(restoredTask -> sink.tryEmitNext(restoredTask));
                });
    }

    public Mono<Task> archiveTask(String id) {
        return taskRepository.findById(id)
                .flatMap(existingTask -> {
                    existingTask.setArchived(true);
                    return taskRepository.save(existingTask)
                        .doOnSuccess(archivedTask -> sink.tryEmitNext(archivedTask));
                });
    }

    public Mono<Task> unarchiveTask(String id) {
        return taskRepository.findById(id)
                .flatMap(existingTask -> {
                    existingTask.setArchived(false);
                    return taskRepository.save(existingTask)
                        .doOnSuccess(unarchivedTask -> sink.tryEmitNext(unarchivedTask));
                });
    }

    public Mono<Void> hardDeleteTask(String id) {
        return taskRepository.deleteById(id);
    }
}
