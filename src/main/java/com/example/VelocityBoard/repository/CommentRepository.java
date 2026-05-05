package com.example.VelocityBoard.repository;

import com.example.VelocityBoard.model.Comment;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CommentRepository extends ReactiveMongoRepository<Comment, String> {
    Flux<Comment> findByTaskIdOrderByCreatedAtAsc(String taskId);
}
