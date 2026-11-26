package repository;

import model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory thread-safe implementation of TaskRepository.
 * Designed as a thread-safe Singleton.
 * Implements cascading deletion for subtasks.
 */
public final class InMemoryTaskRepository implements TaskRepository {

    private static volatile InMemoryTaskRepository instance;
    private final Map<Integer, Task> tasks = Collections.synchronizedMap(new LinkedHashMap<>());
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    // Private constructor enforces Singleton pattern
    private InMemoryTaskRepository() {
    }

    public static InMemoryTaskRepository getInstance() {
        if (instance == null) {
            synchronized (InMemoryTaskRepository.class) {
                if (instance == null) {
                    instance = new InMemoryTaskRepository();
                }
            }
        }
        return instance;
    }

    @Override
    public synchronized Task save(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task to save cannot be null");
        }

        int targetId = task.getId();
        if (targetId <= 0) {
            targetId = idGenerator.getAndIncrement();
            Task saved = task.toBuilder().id(targetId).build();
            tasks.put(targetId, saved);
            return saved;
        } else {
            tasks.put(targetId, task);
            return task;
        }
    }

    @Override
    public Optional<Task> findById(int id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public List<Task> findAll() {
        synchronized (tasks) {
            return new ArrayList<>(tasks.values());
        }
    }

    @Override
    public synchronized boolean delete(int id) {
        Task removed = tasks.remove(id);
        if (removed == null) {
            return false;
        }

        // Cascading delete: find and delete all child subtasks
        List<Integer> subtaskIds = new ArrayList<>();
        synchronized (tasks) {
            for (Task t : tasks.values()) {
                if (t.getParentId() != null && t.getParentId() == id) {
                    subtaskIds.add(t.getId());
                }
            }
        }

        for (int subtaskId : subtaskIds) {
            delete(subtaskId); // recursive cascade if needed
        }

        return true;
    }

    @Override
    public List<Task> findByParentId(int parentId) {
        List<Task> subtasks = new ArrayList<>();
        synchronized (tasks) {
            for (Task t : tasks.values()) {
                if (t.getParentId() != null && t.getParentId() == parentId) {
                    subtasks.add(t);
                }
            }
        }
        return subtasks;
    }

    @Override
    public synchronized void clear() {
        tasks.clear();
        idGenerator.set(1);
    }
}
