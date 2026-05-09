package com.example.VelocityBoard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "task_activities")
public class TaskActivity {
    @Id
    private String id;
    private String taskId;
    private String username;
    private String action;
    private Date timestamp;
}
