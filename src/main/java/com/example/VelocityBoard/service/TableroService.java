package com.example.VelocityBoard.service;

import com.example.VelocityBoard.model.Tablero;
import com.example.VelocityBoard.repository.TableroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
@RequiredArgsConstructor
public class TableroService {

    private final TableroRepository tableroRepository;
    private final Sinks.Many<Tablero> sink = Sinks.many().multicast().onBackpressureBuffer();

    public Mono<Tablero> crearTablero(String nombre, String descripcion, String propietarioId) {
        Tablero tablero = Tablero.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .propietarioId(propietarioId)
                .build();
        return tableroRepository.save(tablero)
                .doOnSuccess(sink::tryEmitNext);
    }

    public Flux<Tablero> listarTablerosPorPropietario(String propietarioId) {
        return tableroRepository.findByPropietarioIdAndEliminadoFalse(propietarioId);
    }

    public Mono<Tablero> obtenerTableroPorId(String id, String userId) {
        return tableroRepository.findById(id)
                .filter(t -> (t.getPropietarioId().equals(userId) || t.getMiembros().contains(userId)) && !t.isEliminado())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tablero no encontrado o sin acceso")));
    }

    public Mono<Tablero> addMember(String tableroId, String userIdToAdd, String currentUserId) {
        return tableroRepository.findById(tableroId)
                .filter(t -> t.getPropietarioId().equals(currentUserId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el propietario puede agregar miembros")))
                .flatMap(t -> {
                    if (!t.getMiembros().contains(userIdToAdd)) {
                        t.getMiembros().add(userIdToAdd);
                    }
                    return tableroRepository.save(t).doOnSuccess(sink::tryEmitNext);
                });
    }

    public Flux<Tablero> getTableroEvents() {
        return sink.asFlux();
    }
}
