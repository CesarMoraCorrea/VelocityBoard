package com.example.VelocityBoard.service;

import com.example.VelocityBoard.model.Task;
import com.example.VelocityBoard.model.TaskActivity;
import com.example.VelocityBoard.repository.TaskActivityRepository;
import com.example.VelocityBoard.repository.TaskRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import java.util.Collection;
import java.util.Date;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final Sinks.Many<Task> sink;

    public TaskService(TaskRepository taskRepository, TaskActivityRepository taskActivityRepository) {
        this.taskRepository = taskRepository;
        this.taskActivityRepository = taskActivityRepository;
        // Use a multicasting sink to broadcast events to all subscribers
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    public Mono<Task> saveAndEmitTask(Task task, String username, String action) {
        return taskRepository.save(task)
                .flatMap(savedTask -> {
                    TaskActivity activity = TaskActivity.builder()
                            .taskId(savedTask.getId())
                            .username(username)
                            .action(action)
                            .timestamp(new Date())
                            .build();
                    return taskActivityRepository.save(activity).thenReturn(savedTask);
                })
                .doOnSuccess(savedTask -> {
                    // Try to emit the event, this broadcasts to all subscribers
                    sink.tryEmitNext(savedTask);
                });
    }

    public Flux<TaskActivity> getTaskHistory(String taskId) {
        return taskActivityRepository.findByTaskIdOrderByTimestampDesc(taskId);
    }

    public Flux<Task> getTaskEvents() {
        return taskRepository.findAll().concatWith(sink.asFlux());
    }

    public Mono<Task> getTaskById(String id) {
        return taskRepository.findById(id);
    }

    public Flux<Task> getTasksByUserId(String userId) {
        return taskRepository.findByUserId(userId);
    }

    public Flux<Task> getTasksByColumnId(String columnId) {
        return taskRepository.findByColumnIdOrderByPositionAsc(columnId);
    }

    public Flux<Task> getDeletedTasksByColumnIds(Collection<String> columnIds) {
        return taskRepository.findByColumnIdInAndDeletedTrue(columnIds);
    }

    public Mono<Task> updateTask(String id, Task updatedTask, String username) {
        return taskRepository.findById(id)
                .flatMap(existingTask -> {
                    if (updatedTask.getTitle() != null) existingTask.setTitle(updatedTask.getTitle());
                    if (updatedTask.getDescription() != null) existingTask.setDescription(updatedTask.getDescription());
                    if (updatedTask.getColumnId() != null) existingTask.setColumnId(updatedTask.getColumnId());
                    if (updatedTask.getTags() != null) existingTask.setTags(updatedTask.getTags());
                    if (updatedTask.getPosition() != null) existingTask.setPosition(updatedTask.getPosition());
                    existingTask.setUpdatedBy(username);
                    existingTask.setUpdatedAt(new Date());
                    return taskRepository.save(existingTask);
                })
                .flatMap(task -> {
                    TaskActivity activity = TaskActivity.builder()
                            .taskId(task.getId()).username(username)
                            .action("editó la tarea").timestamp(new Date()).build();
                    return taskActivityRepository.save(activity).thenReturn(task);
                })
                .doOnSuccess(task -> sink.tryEmitNext(task));
    }

    public Mono<Task> softDeleteTask(String id, String username) {
        return taskRepository.findById(id)
                .flatMap(existingTask -> {
                    existingTask.setDeleted(true);
                    existingTask.setUpdatedBy(username);
                    existingTask.setUpdatedAt(new Date());
                    return taskRepository.save(existingTask);
                })
                .flatMap(task -> {
                    TaskActivity activity = TaskActivity.builder()
                            .taskId(task.getId()).username(username)
                            .action("envió la tarea a la papelera").timestamp(new Date()).build();
                    return taskActivityRepository.save(activity).thenReturn(task);
                })
                .doOnSuccess(deletedTask -> sink.tryEmitNext(deletedTask));
    }

    public Mono<Task> restoreTask(String id, String username) {
        return taskRepository.findById(id)
                .flatMap(existingTask -> {
                    existingTask.setDeleted(false);
                    existingTask.setUpdatedBy(username);
                    existingTask.setUpdatedAt(new Date());
                    return taskRepository.save(existingTask);
                })
                .flatMap(task -> {
                    TaskActivity activity = TaskActivity.builder()
                            .taskId(task.getId()).username(username)
                            .action("restauró la tarea").timestamp(new Date()).build();
                    return taskActivityRepository.save(activity).thenReturn(task);
                })
                .doOnSuccess(restoredTask -> sink.tryEmitNext(restoredTask));
    }

    public Mono<Task> archiveTask(String id, String username) {
        return taskRepository.findById(id)
                .flatMap(existingTask -> {
                    existingTask.setArchived(true);
                    existingTask.setUpdatedBy(username);
                    existingTask.setUpdatedAt(new Date());
                    return taskRepository.save(existingTask);
                })
                .flatMap(task -> {
                    TaskActivity activity = TaskActivity.builder()
                            .taskId(task.getId()).username(username)
                            .action("archivó la tarea").timestamp(new Date()).build();
                    return taskActivityRepository.save(activity).thenReturn(task);
                })
                .doOnSuccess(archivedTask -> sink.tryEmitNext(archivedTask));
    }

    public Mono<Task> unarchiveTask(String id, String username) {
        return taskRepository.findById(id)
                .flatMap(existingTask -> {
                    existingTask.setArchived(false);
                    existingTask.setUpdatedBy(username);
                    existingTask.setUpdatedAt(new Date());
                    return taskRepository.save(existingTask);
                })
                .flatMap(task -> {
                    TaskActivity activity = TaskActivity.builder()
                            .taskId(task.getId()).username(username)
                            .action("desarchivó la tarea").timestamp(new Date()).build();
                    return taskActivityRepository.save(activity).thenReturn(task);
                })
                .doOnSuccess(unarchivedTask -> sink.tryEmitNext(unarchivedTask));
    }

    public Mono<Void> hardDeleteTask(String id, String username) {
        return taskActivityRepository.findByTaskIdOrderByTimestampDesc(id)
                .flatMap(taskActivityRepository::delete)
                .then(taskRepository.deleteById(id));
    }

    public Mono<Task> duplicateTask(String id, String targetColumnId, String username) {
        return taskRepository.findById(id)
                .flatMap(originalTask -> {
                    Task newTask = new Task();
                    newTask.setTitle("Copia de " + originalTask.getTitle());
                    newTask.setDescription(originalTask.getDescription());
                    newTask.setTags(originalTask.getTags());
                    newTask.setUserId(originalTask.getUserId());
                    newTask.setPosition(originalTask.getPosition());
                    
                    if (targetColumnId != null && !targetColumnId.isEmpty()) {
                        newTask.setColumnId(targetColumnId);
                    } else {
                        newTask.setColumnId(originalTask.getColumnId());
                    }
                    
                    newTask.setCreatedAt(new Date());
                    newTask.setCreatedBy(username);
                    newTask.setUpdatedBy(username);
                    newTask.setUpdatedAt(new Date());
                    newTask.setDeleted(false);
                    newTask.setArchived(false);
                    
                    return taskRepository.save(newTask);
                })
                .flatMap(task -> {
                    TaskActivity activity = TaskActivity.builder()
                            .taskId(task.getId()).username(username)
                            .action("duplicó la tarea").timestamp(new Date()).build();
                    return taskActivityRepository.save(activity).thenReturn(task);
                })
                .doOnSuccess(savedTask -> sink.tryEmitNext(savedTask));
    }
}
