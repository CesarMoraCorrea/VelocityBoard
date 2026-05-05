package com.example.VelocityBoard.service;

import com.example.VelocityBoard.model.Tablero;
import com.example.VelocityBoard.repository.TableroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TableroService {

    private final TableroRepository tableroRepository;

    public Mono<Tablero> crearTablero(String nombre, String descripcion, String propietarioId) {
        Tablero tablero = Tablero.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .propietarioId(propietarioId)
                .build();
        return tableroRepository.save(tablero);
    }

    public Flux<Tablero> listarTablerosPorPropietario(String propietarioId) {
        return tableroRepository.findByPropietarioIdAndEliminadoFalse(propietarioId);
    }

    public Mono<Tablero> obtenerTableroPorId(String id, String propietarioId) {
        return tableroRepository.findById(id)
                .filter(t -> t.getPropietarioId().equals(propietarioId) && !t.isEliminado())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tablero no encontrado")));
    }
}
