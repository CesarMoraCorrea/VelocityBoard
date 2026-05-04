package com.example.VelocityBoard.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "columns")
public class Column {
    @Id
    private String id;
    private String name;
    private String userId;
    private Integer position;
}
