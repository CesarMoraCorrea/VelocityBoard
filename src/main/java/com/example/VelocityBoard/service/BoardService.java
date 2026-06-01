package com.example.VelocityBoard.service;

import com.example.VelocityBoard.repository.ColumnRepository;
import com.example.VelocityBoard.repository.CommentRepository;
import com.example.VelocityBoard.repository.TableroRepository;
import com.example.VelocityBoard.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final TableroRepository tableroRepository;
    private final ColumnRepository columnRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;

    public Mono<Void> deleteBoard(String boardId, String userId) {
        return tableroRepository.findById(boardId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tablero no encontrado")))
                .filter(tablero -> tablero.getPropietarioId().equals(userId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el propietario puede eliminar este tablero")))
                .flatMap(tablero -> Mono.when(
                        tableroRepository.delete(tablero),
                        columnRepository.deleteByTableroId(boardId),
                        taskRepository.deleteByTableroId(boardId),
                        commentRepository.deleteByTableroId(boardId)
                ));
    }

    public Mono<com.example.VelocityBoard.model.Tablero> renameBoard(String boardId, String newName, String userId) {
        return tableroRepository.findById(boardId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tablero no encontrado")))
                .filter(tablero -> tablero.getPropietarioId().equals(userId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el propietario puede renombrar este tablero")))
                .flatMap(tablero -> {
                    tablero.setNombre(newName);
                    return tableroRepository.save(tablero);
                });
    }
}
