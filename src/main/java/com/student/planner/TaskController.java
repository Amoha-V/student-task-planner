package com.student.planner;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) { this.service = service; }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }

    @GetMapping("/tasks")
    public List<Task> list(@RequestParam(required = false) String q,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String priority) {
        return service.list(q, status, priority);
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public Task create(@RequestBody TaskRequest r) { return service.create(r); }

    @PutMapping("/tasks/{id}")
    public Task update(@PathVariable long id, @RequestBody TaskRequest r) { return service.update(id, r); }

    @PatchMapping("/tasks/{id}/complete")
    public Task toggle(@PathVariable long id) { return service.toggleComplete(id); }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }
}
