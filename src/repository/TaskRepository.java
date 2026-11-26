package repository;

import model.Task;

import java.util.List;
import java.util.Optional;

/**
 * Interface for task persistence layer, adhering to Dependency Inversion Principle.
 */
public interface TaskRepository {

    /**
     * Saves a task. If task.getId() <= 0, generates a new ID and persists it.
     * If task has an existing ID, updates the task record.
     */
    Task save(Task task);

    /**
     * Retrieves a task by its unique ID.
     */
    Optional<Task> findById(int id);

    /**
     * Retrieves all tasks currently stored in repository.
     */
    List<Task> findAll();

    /**
     * Deletes a task by ID. Cascades deletion to any child subtasks.
     * Returns true if task was found and deleted, false otherwise.
     */
    boolean delete(int id);

    /**
     * Retrieves all direct subtasks of a given parent task.
     */
    List<Task> findByParentId(int parentId);

    /**
     * Clears all tasks from the repository (useful for test isolation).
     */
    void clear();
}
