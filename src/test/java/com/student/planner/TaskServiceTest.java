package com.student.planner;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class TaskServiceTest {
    private final TaskService service = new TaskService();

    private TaskRequest req(String title, String priority) {
        return new TaskRequest(title, "desc", priority, "2026-12-01");
    }

    @Test
    void createsTask() {
        Task t = service.create(req("Write report", "high"));
        assertEquals(1, t.getId());
        assertEquals("HIGH", t.getPriority());
        assertFalse(t.isCompleted());
        assertEquals(1, service.list(null, null, null).size());
    }

    @Test
    void togglesCompletion() {
        Task t = service.create(req("Study", "LOW"));
        assertTrue(service.toggleComplete(t.getId()).isCompleted());
        assertEquals(1, service.list(null, "completed", null).size());
        assertEquals(0, service.list(null, "pending", null).size());
    }

    @Test
    void deletesTask() {
        Task t = service.create(req("Temp", "MEDIUM"));
        service.delete(t.getId());
        assertTrue(service.list(null, null, null).isEmpty());
    }

    @Test
    void missingTaskReturns404() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.delete(99));
        assertEquals(404, e.getStatusCode().value());
    }

    @Test
    void invalidPriorityReturns400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> service.create(req("Bad", "URGENT")));
        assertEquals(400, e.getStatusCode().value());
    }

    @Test
    void searchFindsByTitle() {
        service.create(req("Complete CI/CD Assignment", "HIGH"));
        service.create(req("Buy notebook", "LOW"));
        assertEquals(1, service.list("ci/cd", null, null).size());
    }
}
