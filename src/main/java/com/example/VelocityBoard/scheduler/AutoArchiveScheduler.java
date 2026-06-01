package com.example.VelocityBoard.scheduler;

import com.example.VelocityBoard.model.Column;
import com.example.VelocityBoard.model.Task;
import com.example.VelocityBoard.repository.ColumnRepository;
import com.example.VelocityBoard.repository.TaskRepository;
import com.example.VelocityBoard.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class AutoArchiveScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoArchiveScheduler.class);

    private final ColumnRepository columnRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskService;

    public AutoArchiveScheduler(ColumnRepository columnRepository, TaskRepository taskRepository, TaskService taskService) {
        this.columnRepository = columnRepository;
        this.taskRepository = taskRepository;
        this.taskService = taskService;
    }

    /**
     * Scheduled method running every day at midnight.
     * Selects all tasks in "Completadas" or "DONE" columns that were completed more than 3 days ago,
     * reactively updates their status to archived, and saves them.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void archiveOldTasks() {
        log.info("[AutoArchiveScheduler] Starting auto-archive scheduled job...");
        taskService.autoArchiveOldTasks()
                .subscribe(
                        archivedTask -> log.info("[AutoArchiveScheduler] Successfully archived task: {} ('{}')", 
                                archivedTask.getId(), archivedTask.getTitle()),
                        error -> log.error("[AutoArchiveScheduler] Error during auto-archiving: ", error),
                        () -> log.info("[AutoArchiveScheduler] Auto-archive scheduled job completed successfully.")
                );
    }
}
