package com.example.VelocityBoard.repository;

import com.example.VelocityBoard.model.Task;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;

@Repository
public interface TaskRepository extends ReactiveMongoRepository<Task, String> {
    Flux<Task> findByUserId(String userId);
    Flux<Task> findByColumnIdOrderByPositionAsc(String columnId);
}
