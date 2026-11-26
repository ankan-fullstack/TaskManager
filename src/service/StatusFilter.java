package service;

import model.Task;
import model.TaskQuery;

public class StatusFilter implements TaskFilter {
    @Override
    public boolean matches(Task task, TaskQuery query) {
        if (query.getCompleted() == null) {
            return true;
        }
        return task.isCompleted() == query.getCompleted();
    }
}
