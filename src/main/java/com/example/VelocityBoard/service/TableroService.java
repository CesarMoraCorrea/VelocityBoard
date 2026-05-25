package com.example.VelocityBoard.service;

import com.example.VelocityBoard.model.Tablero;
import com.example.VelocityBoard.repository.TableroRepository;
import com.example.VelocityBoard.repository.UserRepository;
import com.example.VelocityBoard.dto.CollaboratorResponse;
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
    private final UserRepository userRepository;
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

    private Mono<Tablero> sanitizarTablero(Tablero tablero) {
        if (tablero.getMiembros() == null || tablero.getMiembros().isEmpty()) {
            return Mono.just(tablero);
        }
        return Flux.fromIterable(tablero.getMiembros())
                .flatMap(miembroId -> userRepository.existsById(miembroId)
                        .map(exists -> exists ? miembroId : null))
                .filter(java.util.Objects::nonNull)
                .collectList()
                .flatMap(validMiembros -> {
                    if (validMiembros.size() != tablero.getMiembros().size()) {
                        tablero.setMiembros(validMiembros);
                        return tableroRepository.save(tablero).doOnSuccess(sink::tryEmitNext);
                    }
                    return Mono.just(tablero);
                });
    }

    public Flux<Tablero> listarTablerosPorPropietario(String propietarioId) {
        return tableroRepository.findByPropietarioIdAndEliminadoFalse(propietarioId)
                .flatMap(this::sanitizarTablero);
    }

    public Mono<Tablero> obtenerTableroPorId(String id, String userId) {
        return tableroRepository.findById(id)
                .filter(t -> (t.getPropietarioId().equals(userId) || t.getMiembros().contains(userId)) && !t.isEliminado())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tablero no encontrado o sin acceso")))
                .flatMap(this::sanitizarTablero);
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

    public Mono<Tablero> removeMember(String tableroId, String userIdToRemove, String currentUserId) {
        return tableroRepository.findById(tableroId)
                .filter(t -> t.getPropietarioId().equals(currentUserId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el propietario puede remover miembros")))
                .flatMap(t -> {
                    if (t.getMiembros().contains(userIdToRemove)) {
                        t.getMiembros().remove(userIdToRemove);
                    }
                    return tableroRepository.save(t).doOnSuccess(sink::tryEmitNext);
                });
    }

    public Flux<Tablero> getTableroEvents() {
        return sink.asFlux();
    }

    public Flux<CollaboratorResponse> listarColaboradores(String tableroId, String currentUserId) {
        return tableroRepository.findById(tableroId)
                .filter(t -> (t.getPropietarioId().equals(currentUserId) || t.getMiembros().contains(currentUserId)) && !t.isEliminado())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tablero no encontrado o sin acceso")))
                .flatMapMany(tablero -> {
                    String propietarioId = tablero.getPropietarioId();
                    java.util.List<String> miembros = tablero.getMiembros() != null ? tablero.getMiembros() : new java.util.ArrayList<>();

                    return userRepository.findById(propietarioId)
                            .map(owner -> CollaboratorResponse.builder()
                                    .id(owner.getId())
                                    .name(owner.getUsername())
                                    .email(owner.getEmail())
                                    .avatarUrl("https://api.dicebear.com/7.x/initials/svg?seed=" + owner.getUsername())
                                    .status("CREATOR")
                                    .build())
                            .flux()
                            .concatWith(
                                    userRepository.findAllById(miembros)
                                            .filter(u -> !u.getId().equals(propietarioId)) // Avoid duplicate if owner is in members
                                            .map(u -> CollaboratorResponse.builder()
                                                    .id(u.getId())
                                                    .name(u.getUsername())
                                                    .email(u.getEmail())
                                                    .avatarUrl("https://api.dicebear.com/7.x/initials/svg?seed=" + u.getUsername())
                                                    .status("MEMBER")
                                                    .build())
                            );
                });
    }
}
