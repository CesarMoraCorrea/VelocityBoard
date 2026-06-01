package com.example.VelocityBoard.service;

import com.example.VelocityBoard.model.Task;
import com.example.VelocityBoard.model.TaskActivity;
import com.example.VelocityBoard.repository.ColumnRepository;
import com.example.VelocityBoard.repository.TaskActivityRepository;
import com.example.VelocityBoard.repository.TaskRepository;
import com.example.VelocityBoard.repository.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final Sinks.Many<Task> sink;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final ColumnRepository columnRepository;

    public TaskService(TaskRepository taskRepository, TaskActivityRepository taskActivityRepository, EmailService emailService, UserRepository userRepository, ColumnRepository columnRepository) {
        this.taskRepository = taskRepository;
        this.taskActivityRepository = taskActivityRepository;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.columnRepository = columnRepository;
        // Use a multicasting sink to broadcast events to all subscribers
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    public Mono<Task> saveAndEmitTask(Task task, String username, String action) {
        System.out.println("[VelocityBoard DEBUG] saveAndEmitTask entry. Title: " + task.getTitle() + ", Assigned Email: " + task.getAssignedUserEmail());
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
                    if (savedTask != null) {
                        String assignedEmail = savedTask.getAssignedUserEmail();
                        System.out.println("[VelocityBoard DEBUG] saveAndEmitTask saved successfully. ID: " + savedTask.getId() + ", Assigned Email: " + assignedEmail);
                        if (assignedEmail != null && !assignedEmail.isEmpty()) {
                            String subject = "Nueva tarea asignada: " + savedTask.getTitle();
                            String htmlBody = "Hello, a new task titled \"" + savedTask.getTitle() + "\" has been assigned to you on VelocityBoard.";
                            System.out.println("[VelocityBoard DEBUG] Attempting to send email to: " + assignedEmail + " with subject: " + subject);
                            emailService.sendHtmlEmail(assignedEmail, subject, htmlBody)
                                    .subscribe(
                                            response -> System.out.println("[VelocityBoard DEBUG] Email sent successfully! Response ID: " + response.getId()),
                                            error -> {
                                                System.err.println("[VelocityBoard DEBUG] Failed to send assignment email: " + error.getMessage());
                                                error.printStackTrace();
                                            }
                                    );
                        } else {
                            System.out.println("[VelocityBoard DEBUG] No assigned user email found for task. Skipping email send.");
                        }
                        sink.tryEmitNext(savedTask);
                    }
                });
    }

    public Flux<TaskActivity> getTaskHistory(String taskId) {
        return taskActivityRepository.findByTaskIdOrderByTimestampDesc(taskId);
    }

    private Mono<Task> sanitizarTask(Task task) {
        if (task.getUserId() == null || task.getUserId().isEmpty()) {
            return Mono.just(task);
        }
        return userRepository.existsById(task.getUserId())
                .flatMap(exists -> {
                    if (!exists) {
                        task.setUserId(null);
                        task.setAssignedUserEmail(null);
                        return taskRepository.save(task).doOnSuccess(sink::tryEmitNext);
                    }
                    return Mono.just(task);
                });
    }

    public Flux<Task> getTaskEvents() {
        return taskRepository.findAll()
                .flatMap(this::sanitizarTask)
                .concatWith(sink.asFlux().flatMap(this::sanitizarTask));
    }

    public Mono<Task> getTaskById(String id) {
        return taskRepository.findById(id).flatMap(this::sanitizarTask);
    }

    public Flux<Task> getTasksByUserId(String userId) {
        return taskRepository.findByUserId(userId).flatMap(this::sanitizarTask);
    }

    public Flux<Task> getTasksByColumnId(String columnId) {
        return taskRepository.findByColumnIdOrderByPositionAsc(columnId).flatMap(this::sanitizarTask);
    }

    public Flux<Task> getDeletedTasksByColumnIds(Collection<String> columnIds) {
        return taskRepository.findByColumnIdInAndDeletedTrue(columnIds).flatMap(this::sanitizarTask);
    }

    public Mono<Task> updateTask(String id, Task updatedTask, String username) {
        System.out.println("[VelocityBoard DEBUG] updateTask entry. ID: " + id + ", Title: " + updatedTask.getTitle() + ", Assigned Email: " + updatedTask.getAssignedUserEmail());
        return taskRepository.findById(id)
                .flatMap(existingTask -> {
                    String oldEmail = existingTask.getAssignedUserEmail();
                    String newEmail = updatedTask.getAssignedUserEmail();
                    System.out.println("[VelocityBoard DEBUG] updateTask processing. Task ID: " + id + ", Old Email: " + oldEmail + ", New Email: " + newEmail);

                    if (updatedTask.getTitle() != null) existingTask.setTitle(updatedTask.getTitle());
                    if (updatedTask.getDescription() != null) existingTask.setDescription(updatedTask.getDescription());
 
                    String oldColumnId = existingTask.getColumnId();
                    String newColumnId = updatedTask.getColumnId();
                    Mono<Void> updateColumnAndStatusMono = Mono.empty();
 
                    if (newColumnId != null && !newColumnId.equals(oldColumnId)) {
                        updateColumnAndStatusMono = columnRepository.findById(newColumnId)
                                .doOnNext(column -> {
                                    existingTask.setColumnId(newColumnId);
                                    String columnName = column.getName();
                                    if (columnName != null && (columnName.equalsIgnoreCase("Completadas") || columnName.equalsIgnoreCase("DONE"))) {
                                        existingTask.setCompletedAt(Instant.now());
                                    } else {
                                        existingTask.setCompletedAt(null);
                                    }
                                })
                                .switchIfEmpty(Mono.fromRunnable(() -> {
                                    existingTask.setColumnId(newColumnId);
                                }))
                                .then();
                    }
 
                    if (updatedTask.getTags() != null) existingTask.setTags(updatedTask.getTags());
                    if (updatedTask.getPosition() != null) existingTask.setPosition(updatedTask.getPosition());
                    if (newEmail != null) {
                        existingTask.setAssignedUserEmail(newEmail.isEmpty() ? null : newEmail);
                    }

                    // Support task assignment / unassignment
                    if (updatedTask.getUserId() != null) {
                        existingTask.setUserId("none".equals(updatedTask.getUserId()) || updatedTask.getUserId().isBlank() ? null : updatedTask.getUserId());
                    }
                    existingTask.setUpdatedBy(username);
                    existingTask.setUpdatedAt(new Date());

                    boolean isNewAssignment = existingTask.getAssignedUserEmail() != null 
                            && !existingTask.getAssignedUserEmail().isEmpty()
                            && !existingTask.getAssignedUserEmail().equals(oldEmail);

                    System.out.println("[VelocityBoard DEBUG] updateTask saving. Is new assignment: " + isNewAssignment);

                    return updateColumnAndStatusMono.then(taskRepository.save(existingTask))
                            .flatMap(savedTask -> {
                                TaskActivity activity = TaskActivity.builder()
                                        .taskId(savedTask.getId()).username(username)
                                        .action("editó la tarea").timestamp(new Date()).build();
                                return taskActivityRepository.save(activity).thenReturn(savedTask);
                            })
                            .doOnSuccess(savedTask -> {
                                if (savedTask != null && isNewAssignment) {
                                    String subject = "Nueva tarea asignada: " + savedTask.getTitle();
                                    String htmlBody = "Hello, a new task titled \"" + savedTask.getTitle() + "\" has been assigned to you on VelocityBoard.";
                                    System.out.println("[VelocityBoard DEBUG] Attempting to send update email to: " + savedTask.getAssignedUserEmail() + " with subject: " + subject);
                                    emailService.sendHtmlEmail(savedTask.getAssignedUserEmail(), subject, htmlBody)
                                            .subscribe(
                                                    response -> System.out.println("[VelocityBoard DEBUG] Email sent successfully on update! Response ID: " + response.getId()),
                                                    error -> {
                                                        System.err.println("[VelocityBoard DEBUG] Failed to send update assignment email: " + error.getMessage());
                                                        error.printStackTrace();
                                                    }
                                            );
                                } else {
                                    System.out.println("[VelocityBoard DEBUG] No new assignment detected or assignee is empty. Skipping update email.");
                                }
                            });
                })
                .doOnSuccess(task -> {
                    if (task != null) {
                        sink.tryEmitNext(task);
                    }
                });
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

    private boolean isCompletedColumn(String name) {
        if (name == null) return false;
        String normalized = name.toLowerCase().trim();
        return normalized.equals("completadas") || 
               normalized.equals("completada") ||
               normalized.equals("done") || 
               normalized.equals("terminado") || 
               normalized.equals("terminada") || 
               normalized.equals("terminadas");
    }

    public Flux<Task> autoArchiveOldTasks() {
        Instant threshold = Instant.now().minus(3, java.time.temporal.ChronoUnit.DAYS);
        return columnRepository.findAll()
                .filter(col -> col.getName() != null && isCompletedColumn(col.getName()))
                .map(com.example.VelocityBoard.model.Column::getId)
                .collectList()
                .flatMapMany(doneColumnIds -> {
                    if (doneColumnIds.isEmpty()) {
                        return Flux.empty();
                    }
                    return taskRepository.findByColumnIdInAndIsArchivedFalseAndDeletedFalse(doneColumnIds);
                })
                .filter(task -> {
                    Instant completedTime = task.getCompletedAt();
                    if (completedTime == null) {
                        Date refDate = task.getUpdatedAt() != null ? task.getUpdatedAt() : task.getCreatedAt();
                        if (refDate != null) {
                            completedTime = refDate.toInstant();
                        }
                    }
                    return completedTime != null && completedTime.isBefore(threshold);
                })
                .flatMap(task -> archiveTask(task.getId(), "System"));
    }
}
