package com.example.VelocityBoard.service;

import com.example.VelocityBoard.model.Comment;
import com.example.VelocityBoard.repository.CommentRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class CommentService {
    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public void saveCommentAsync(Comment comment) {
        if (comment.getCreatedAt() == null) {
            comment.setCreatedAt(new java.util.Date());
        }
        if (comment.getTableroId() == null || comment.getTableroId().isEmpty()) {
            comment.setTableroId("Tablero General");
        }
        // Non-blocking fire-and-forget save
        commentRepository.save(comment).subscribe();
    }

    public Flux<Comment> getCommentsByTaskId(String taskId) {
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }
}
