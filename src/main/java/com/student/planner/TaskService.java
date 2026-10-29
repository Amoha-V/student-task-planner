package com.student.planner;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskService {
    private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH");
    private final Map<Long, Task> tasks = new LinkedHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    public synchronized List<Task> list(String q, String status, String priority) {
        return tasks.values().stream()
            .filter(t -> q == null || q.isBlank()
                || t.getTitle().toLowerCase().contains(q.toLowerCase())
                || t.getDescription().toLowerCase().contains(q.toLowerCase()))
            .filter(t -> status == null || status.isBlank() || status.equalsIgnoreCase("all")
                || (status.equalsIgnoreCase("completed") == t.isCompleted()))
            .filter(t -> priority == null || priority.isBlank() || priority.equalsIgnoreCase("all")
                || t.getPriority().equalsIgnoreCase(priority))
            .sorted(Comparator.comparing(Task::isCompleted)
                .thenComparing(t -> t.getDueDate().isEmpty() ? "9999" : t.getDueDate())
                .thenComparingLong(Task::getId))
            .collect(Collectors.toList());
    }

    public synchronized Task create(TaskRequest r) {
        Task t = new Task();
        t.setId(ids.incrementAndGet());
        apply(t, r);
        tasks.put(t.getId(), t);
        return t;
    }

    public synchronized Task update(long id, TaskRequest r) {
        Task t = get(id);
        apply(t, r);
        return t;
    }

    public synchronized Task toggleComplete(long id) {
        Task t = get(id);
        t.setCompleted(!t.isCompleted());
        return t;
    }

    public synchronized void delete(long id) {
        get(id);
        tasks.remove(id);
    }

    private Task get(long id) {
        Task t = tasks.get(id);
        if (t == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task " + id + " not found");
        return t;
    }

    private void apply(Task t, TaskRequest r) {
        if (r == null || r.title() == null || r.title().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        String p = r.priority() == null ? "MEDIUM" : r.priority().trim().toUpperCase();
        if (!PRIORITIES.contains(p))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Priority must be LOW, MEDIUM or HIGH");
        t.setTitle(r.title().trim());
        t.setDescription(r.description() == null ? "" : r.description().trim());
        t.setPriority(p);
        t.setDueDate(r.dueDate() == null ? "" : r.dueDate().trim());
    }
}
