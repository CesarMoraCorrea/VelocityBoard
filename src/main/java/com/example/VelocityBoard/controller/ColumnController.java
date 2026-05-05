package com.example.VelocityBoard.controller;

import com.example.VelocityBoard.model.Column;
import com.example.VelocityBoard.service.ColumnService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/columns")
public class ColumnController {

    private final ColumnService columnService;

    public ColumnController(ColumnService columnService) {
        this.columnService = columnService;
    }

    @GetMapping("/user/{userId}")
    public Flux<Column> getColumnsByUserId(@PathVariable String userId) {
        return columnService.getColumnsByUserId(userId);
    }

    @GetMapping("/tablero/{tableroId}")
    public Flux<Column> getColumnsByTableroId(@PathVariable String tableroId) {
        return columnService.getColumnsByTableroId(tableroId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Column> createColumn(@RequestBody Column column) {
        return columnService.createColumn(column);
    }

    @PutMapping("/{id}")
    public Mono<Column> updateColumn(@PathVariable String id, @RequestBody Column column) {
        return columnService.updateColumn(id, column);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<String>> deleteColumn(@PathVariable String id) {
        return columnService.deleteColumn(id)
                .then(Mono.<ResponseEntity<String>>just(ResponseEntity.noContent().build()))
                .onErrorResume(IllegalStateException.class, e ->
                        Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }
}
