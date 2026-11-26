package event;

import model.Task;

/**
 * Observer interface for reacting to task lifecycle events.
 */
public interface TaskEventListener {

    // Event type constants
    String CREATED = "TASK_CREATED";
    String UPDATED = "TASK_UPDATED";
    String COMPLETED = "TASK_COMPLETED";
    String DELETED = "TASK_DELETED";
    String SUBTASK_CREATED = "SUBTASK_CREATED";

    /**
     * Callback triggered when a task event occurs.
     *
     * @param eventType the lifecycle event identifier
     * @param task      the task subject of the event
     */
    void onEvent(String eventType, Task task);
}
