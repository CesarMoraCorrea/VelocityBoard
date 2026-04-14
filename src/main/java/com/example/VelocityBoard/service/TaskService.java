package com.example.VelocityBoard.service;

import com.example.VelocityBoard.model.Task;
import com.example.VelocityBoard.repository.TaskRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final Sinks.Many<Task> sink;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
        // Use a multicasting sink to broadcast events to all subscribers
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    public Mono<Task> saveAndEmitTask(Task task) {
        return taskRepository.save(task)
                .doOnSuccess(savedTask -> {
                    // Try to emit the event, this broadcasts to all subscribers
                    sink.tryEmitNext(savedTask);
                });
    }

    public Flux<Task> getTaskEvents() {
        return sink.asFlux();
    }
}
