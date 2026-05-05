package com.example.VelocityBoard.service;

import com.example.VelocityBoard.model.Column;
import com.example.VelocityBoard.repository.ColumnRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ColumnService {

    private final ColumnRepository columnRepository;
    private final com.example.VelocityBoard.repository.TaskRepository taskRepository;

    public ColumnService(ColumnRepository columnRepository, com.example.VelocityBoard.repository.TaskRepository taskRepository) {
        this.columnRepository = columnRepository;
        this.taskRepository = taskRepository;
    }

    public Flux<Column> getColumnsByUserId(String userId) {
        return columnRepository.findByUserIdOrderByPositionAsc(userId);
    }

    public Flux<Column> getColumnsByTableroId(String tableroId) {
        return columnRepository.findByTableroIdOrderByPositionAsc(tableroId);
    }

    public Mono<Column> createColumn(Column column) {
        return columnRepository.save(column);
    }

    public Mono<Column> updateColumn(String id, Column updatedColumn) {
        return columnRepository.findById(id)
                .flatMap(existingColumn -> {
                    existingColumn.setName(updatedColumn.getName());
                    existingColumn.setPosition(updatedColumn.getPosition());
                    return columnRepository.save(existingColumn);
                });
    }

    public Mono<Void> deleteColumn(String id) {
        return taskRepository.findByColumnIdOrderByPositionAsc(id)
                .hasElements()
                .flatMap(hasTasks -> {
                    if (hasTasks) {
                        return Mono.error(new IllegalStateException("No puedes eliminar una columna que contiene tareas."));
                    }
                    return columnRepository.deleteById(id);
                });
    }
}
