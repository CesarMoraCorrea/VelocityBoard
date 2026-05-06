package com.example.VelocityBoard.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "tasks")
public class Task {
    @Id
    private String id;
    private String userId;
    private String columnId;
    private String tableroId;
    private String title;
    private String description;
    private List<String> tags;
    private Integer position;
    private boolean deleted = false;
    private boolean isArchived = false;
    private Date createdAt = new Date();
}
