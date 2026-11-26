package service;

import model.Task;
import model.TaskQuery;

import java.util.Objects;

public class ParentIdFilter implements TaskFilter {
    @Override
    public boolean matches(Task task, TaskQuery query) {
        if (query.getParentId() == null) {
            return true;
        }
        return Objects.equals(query.getParentId(), task.getParentId());
    }
}
