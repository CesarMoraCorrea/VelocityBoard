package com.example.VelocityBoard.repository;

import com.example.VelocityBoard.model.TaskActivity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TaskActivityRepository extends ReactiveMongoRepository<TaskActivity, String> {
    Flux<TaskActivity> findByTaskIdOrderByTimestampDesc(String taskId);
}
