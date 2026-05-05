package com.example.VelocityBoard.controller;

import com.example.VelocityBoard.model.Comment;
import com.example.VelocityBoard.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public Mono<ResponseEntity<Void>> createComment(@RequestBody Comment comment) {
        commentService.saveCommentAsync(comment);
        return Mono.just(ResponseEntity.accepted().build());
    }

    @GetMapping("/task/{taskId}")
    public Flux<Comment> getCommentsByTask(@PathVariable String taskId) {
        return commentService.getCommentsByTaskId(taskId);
    }
}
