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
@Document(collection = "comments")
public class Comment {
    @Id
    private String id;
    
    private String taskId;
    
    private String authorId;
    
    private String authorName; // Helpful for displaying in the UI without extra joins
    
    private String content;
    
    @Builder.Default
    private String tableroId = "Tablero General";
    
    @Builder.Default
    private Date createdAt = new Date();
}
