package com.example.VelocityBoard.repository;

import com.example.VelocityBoard.model.VerificationToken;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface VerificationTokenRepository extends ReactiveMongoRepository<VerificationToken, String> {
    Mono<VerificationToken> findByToken(String token);
}
