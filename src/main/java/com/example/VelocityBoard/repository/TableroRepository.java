package com.example.VelocityBoard.repository;

import com.example.VelocityBoard.model.Tablero;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TableroRepository extends ReactiveMongoRepository<Tablero, String> {

    Flux<Tablero> findByPropietarioIdAndEliminadoFalse(String propietarioId);
}
