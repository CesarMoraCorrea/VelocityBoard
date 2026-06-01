package com.example.VelocityBoard;

import com.example.VelocityBoard.model.Column;
import com.example.VelocityBoard.model.Task;
import com.example.VelocityBoard.repository.ColumnRepository;
import com.example.VelocityBoard.repository.TaskRepository;
import com.example.VelocityBoard.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@SpringBootTest
class ApplicationTests {

    static {
        try {
            io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
            dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        } catch (Exception e) {
            System.out.println("No .env file loaded in tests");
        }
    }

    @Autowired
    private ColumnRepository columnRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @Test
    void testCheckAndArchive() {
        System.out.println("===== DIAGNOSTIC TEST START =====");
        
        columnRepository.findAll()
                .doOnNext(col -> System.out.println("Column: ID=" + col.getId() + ", Name='" + col.getName() + "'"))
                .collectList()
                .flatMap(cols -> {
                    System.out.println("Total columns in DB: " + cols.size());
                    return taskRepository.findAll().collectList();
                })
                .flatMap(tasks -> {
                    System.out.println("Total tasks in DB: " + tasks.size());
                    for (Task t : tasks) {
                        System.out.println("Task: ID=" + t.getId() + 
                                ", Title='" + t.getTitle() + "'" +
                                ", ColumnId=" + t.getColumnId() +
                                ", isArchived=" + t.isArchived() +
                                ", deleted=" + t.isDeleted() +
                                ", completedAt=" + t.getCompletedAt() +
                                ", updatedAt=" + t.getUpdatedAt() +
                                ", createdAt=" + t.getCreatedAt());
                    }
                    
                    System.out.println("Executing autoArchiveOldTasks...");
                    return taskService.autoArchiveOldTasks().collectList();
                })
                .doOnNext(archived -> {
                    System.out.println("===== ARCHIVED " + archived.size() + " TASKS =====");
                    for (Task t : archived) {
                        System.out.println("Archived Task: " + t.getTitle());
                    }
                })
                .block();
                
        System.out.println("===== DIAGNOSTIC TEST END =====");
    }
}
