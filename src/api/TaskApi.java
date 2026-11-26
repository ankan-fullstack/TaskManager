package api;

import exception.ApiException;
import model.TaskQuery;
import model.TaskRequest;
import repository.InMemoryTaskRepository;
import service.TaskService;

/**
 * Public API boundary simulating REST endpoints without any web framework.
 * Every operation delegates to TaskService, intercepts any Exceptions,
 * and always guarantees a structured ApiResponse.
 */
public class TaskApi {

    private final TaskService taskService;

    public TaskApi() {
        this(new TaskService(InMemoryTaskRepository.getInstance()));
    }

    public TaskApi(TaskService taskService) {
        if (taskService == null) {
            throw new IllegalArgumentException("TaskService cannot be null");
        }
        this.taskService = taskService;
    }

    public TaskService getService() {
        return taskService;
    }

    public ApiResponse createTask(TaskRequest request) {
        return createTask(request, null);
    }

    public ApiResponse createTask(TaskRequest request, String idempotencyKey) {
        try {
            return taskService.createTask(request, idempotencyKey);
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.from(e);
        }
    }

    public ApiResponse getTask(int id) {
        try {
            return taskService.getTask(id);
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.from(e);
        }
    }

    public ApiResponse getAllTasks() {
        try {
            return taskService.getAllTasks();
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.from(e);
        }
    }

    public ApiResponse updateTask(int id, TaskRequest request) {
        try {
            return taskService.updateTask(id, request);
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.from(e);
        }
    }

    public ApiResponse deleteTask(int id) {
        try {
            return taskService.deleteTask(id);
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.from(e);
        }
    }

    public ApiResponse createTaskFromText(String text) {
        return createTaskFromText(text, null);
    }

    public ApiResponse createTaskFromText(String text, String idempotencyKey) {
        try {
            return taskService.createTaskFromText(text, idempotencyKey);
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.from(e);
        }
    }

    public ApiResponse createSubtask(int parentId, TaskRequest request) {
        try {
            return taskService.createSubtask(parentId, request);
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.from(e);
        }
    }

    public ApiResponse listSubtasks(int parentId) {
        try {
            return taskService.listSubtasks(parentId);
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.from(e);
        }
    }

    public ApiResponse breakDownTask(int id) {
        return breakDownTask(id, null);
    }

    public ApiResponse breakDownTask(int id, String idempotencyKey) {
        try {
            return taskService.breakDownTask(id, idempotencyKey);
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.from(e);
        }
    }

    public ApiResponse queryTasks(TaskQuery query) {
        try {
            return taskService.queryTasks(query);
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.from(e);
        }
    }
}
