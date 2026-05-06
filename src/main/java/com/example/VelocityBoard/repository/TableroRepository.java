package com.example.VelocityBoard.repository;

import com.example.VelocityBoard.model.Tablero;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TableroRepository extends ReactiveMongoRepository<Tablero, String> {

    @Query("{ '$or': [ { 'propietarioId': ?0 }, { 'miembros': ?0 } ], 'eliminado': false }")
    Flux<Tablero> findByPropietarioIdAndEliminadoFalse(String propietarioId);
}
