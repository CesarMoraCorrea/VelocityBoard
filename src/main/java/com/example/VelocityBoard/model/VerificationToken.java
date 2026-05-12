package com.example.VelocityBoard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "verification_tokens")
public class VerificationToken {

    @Id
    private String id;

    private String token;
    
    private String userId;

    private LocalDateTime expiryDate;
}
