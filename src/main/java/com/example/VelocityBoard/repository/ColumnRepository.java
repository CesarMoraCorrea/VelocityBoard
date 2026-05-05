package com.example.VelocityBoard.repository;

import com.example.VelocityBoard.model.Column;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ColumnRepository extends ReactiveMongoRepository<Column, String> {
    Flux<Column> findByUserIdOrderByPositionAsc(String userId);
    Flux<Column> findByTableroIdOrderByPositionAsc(String tableroId);
}
