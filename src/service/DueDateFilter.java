package service;

import model.Task;
import model.TaskQuery;

import java.util.Objects;

public class DueDateFilter implements TaskFilter {
    @Override
    public boolean matches(Task task, TaskQuery query) {
        if (query.getDueDate() == null || query.getDueDate().trim().isEmpty()) {
            return true;
        }
        return Objects.equals(query.getDueDate().trim(), task.getDueDate());
    }
}
