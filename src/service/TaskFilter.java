package service;

import model.Task;
import model.TaskQuery;

/**
 * Filter strategy interface adhering to the Open/Closed Principle.
 * New filtering dimensions can be introduced without modifying existing classes.
 */
public interface TaskFilter {

    /**
     * Determines whether the given task satisfies this filter according to query criteria.
     */
    boolean matches(Task task, TaskQuery query);
}
