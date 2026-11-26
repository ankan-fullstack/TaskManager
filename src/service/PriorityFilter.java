package service;

import model.Task;
import model.TaskQuery;

public class PriorityFilter implements TaskFilter {
    @Override
    public boolean matches(Task task, TaskQuery query) {
        if (query.getPriority() == null) {
            return true;
        }
        return task.getPriority() == query.getPriority();
    }
}
