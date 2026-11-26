package service;

import model.Task;
import model.TaskRequest;

import java.util.List;

/**
 * Strategy interface for breaking down a task into actionable subtasks.
 */
public interface BreakdownStrategy {

    /**
     * Deconstructs a parent task into a list of subtask requests.
     *
     * @param parentTask the task to break down
     * @return list of subtask requests
     * @throws exception.ApiException if breakdown or external model fails
     */
    List<TaskRequest> breakDown(Task parentTask);
}
