package com.example.VelocityBoard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorResponse {
    private String id;
    private String name;
    private String email;
    private String avatarUrl;
    private String status; // "CREATOR" or "MEMBER"
}
