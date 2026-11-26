package service;

import api.ApiResponse;
import event.TaskEventListener;
import exception.ApiException;
import exception.BadGatewayException;
import exception.ConflictException;
import exception.FieldIssue;
import exception.NotFoundException;
import exception.ValidationException;
import model.ParsedTask;
import model.Priority;
import model.Task;
import model.TaskQuery;
import model.TaskRequest;
import parser.DateValidator;
import parser.ParserFactory;
import parser.TaskParser;
import repository.TaskRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Core business service implementing task lifecycle management, validation,
 * AI parsing & breakdown, idempotency, filtering/sorting/pagination,
 * and Observer event dispatching.
 */
public class TaskService {

    private final TaskRepository repository;
    private final TaskParser parser;
    private final BreakdownStrategy breakdownStrategy;
    private final IdempotencyStore idempotencyStore;

    // Observer pattern: Subject maintaining active listeners
    private final List<TaskEventListener> listeners = new CopyOnWriteArrayList<>();

    // Filter pipeline adhering to Open/Closed Principle
    private final List<TaskFilter> filters = Arrays.asList(
            new StatusFilter(),
            new PriorityFilter(),
            new DueDateFilter(),
            new SearchFilter(),
            new ParentIdFilter()
    );

    public TaskService(TaskRepository repository) {
        this(repository, ParserFactory.createDefaultParser(), BreakdownStrategyFactory.createDefaultStrategy(), IdempotencyStore.getInstance());
    }

    public TaskService(TaskRepository repository, TaskParser parser, BreakdownStrategy breakdownStrategy, IdempotencyStore idempotencyStore) {
        this.repository = Objects.requireNonNull(repository, "TaskRepository cannot be null");
        this.parser = parser != null ? parser : ParserFactory.createDefaultParser();
        this.breakdownStrategy = breakdownStrategy != null ? breakdownStrategy : BreakdownStrategyFactory.createDefaultStrategy();
        this.idempotencyStore = idempotencyStore != null ? idempotencyStore : IdempotencyStore.getInstance();
    }

    // ==========================================
    // Observer Pattern: Listener Registration
    // ==========================================

    public void addListener(TaskEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(TaskEventListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners(String eventType, Task task) {
        for (TaskEventListener listener : listeners) {
            try {
                listener.onEvent(eventType, task);
            } catch (Exception e) {
                System.err.println("Listener error on event " + eventType + ": " + e.getMessage());
            }
        }
    }

    // ==========================================
    // Validation Layer (Step 3)
    // ==========================================

    public void validateRequest(TaskRequest request) {
        if (request == null) {
            throw new ValidationException(Collections.singletonList(new FieldIssue("body", "Request body must not be null")));
        }

        List<FieldIssue> issues = new ArrayList<>();

        // 1. title must not be empty or blank
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            issues.add(new FieldIssue("title", "must not be empty"));
        }

        // 2. description must not be empty or blank
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            issues.add(new FieldIssue("description", "must not be empty"));
        }

        // 3. completed must be exactly "true" or "false"
        if (request.getCompleted() == null) {
            issues.add(new FieldIssue("completed", "must be exactly 'true' or 'false'"));
        } else {
            String c = request.getCompleted().trim();
            if (!"true".equals(c) && !"false".equals(c)) {
                issues.add(new FieldIssue("completed", "must be exactly 'true' or 'false'"));
            }
        }

        // 4. dueDate, if given, must be a real YYYY-MM-DD date
        if (request.getDueDate() != null && !request.getDueDate().trim().isEmpty()) {
            if (!DateValidator.isValidDate(request.getDueDate())) {
                issues.add(new FieldIssue("dueDate", "must be a real YYYY-MM-DD date"));
            }
        }

        // 5. priority must be LOW, MEDIUM, or HIGH
        if (request.getPriority() != null && !request.getPriority().trim().isEmpty()) {
            if (!Priority.isValid(request.getPriority())) {
                issues.add(new FieldIssue("priority", "must be LOW, MEDIUM, or HIGH"));
            }
        }

        if (!issues.isEmpty()) {
            throw new ValidationException(issues);
        }
    }

    // ==========================================
    // CRUD Operations (Step 2 & Step 6)
    // ==========================================

    public ApiResponse createTask(TaskRequest request, String idempotencyKey) {
        // Step 6: Idempotency check
        String fingerprint = null;
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            fingerprint = "createTask:" + (request != null ? (request.getTitle() + "|" + request.getDescription() + "|" + request.getCompleted() + "|" + request.getDueDate() + "|" + request.getPriority()) : "null");
            Optional<IdempotencyRecord> existing = idempotencyStore.get(idempotencyKey);
            if (existing.isPresent()) {
                if (existing.get().getFingerprint().equals(fingerprint)) {
                    return existing.get().getResponse();
                } else {
                    throw new ConflictException("Idempotency key '" + idempotencyKey + "' was already used with a different request payload");
                }
            }
        }

        validateRequest(request);

        boolean completed = Boolean.parseBoolean(request.getCompleted().trim());
        Priority priority = request.getPriority() != null && !request.getPriority().trim().isEmpty()
                ? Priority.fromString(request.getPriority())
                : Priority.MEDIUM;

        Task task = Task.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .completed(completed)
                .dueDate(request.getDueDate() != null && !request.getDueDate().trim().isEmpty() ? request.getDueDate().trim() : null)
                .priority(priority)
                .recurrence(request.getRecurrence() != null && !request.getRecurrence().trim().isEmpty() ? request.getRecurrence().trim() : null)
                .createdAt(Instant.now().toString())
                .build();

        Task saved = repository.save(task);
        notifyListeners(TaskEventListener.CREATED, saved);

        ApiResponse response = ApiResponse.created(saved.toJson());
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            idempotencyStore.store(idempotencyKey, fingerprint, response);
        }
        return response;
    }

    public ApiResponse getTask(int id) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task with ID " + id + " not found"));
        return ApiResponse.ok(task.toJson());
    }

    public ApiResponse getAllTasks() {
        List<Task> tasks = repository.findAll();
        return ApiResponse.ok(tasksToJson(tasks));
    }

    public ApiResponse updateTask(int id, TaskRequest request) {
        Task existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task with ID " + id + " not found"));

        validateRequest(request);

        boolean newCompleted = Boolean.parseBoolean(request.getCompleted().trim());
        Priority priority = request.getPriority() != null && !request.getPriority().trim().isEmpty()
                ? Priority.fromString(request.getPriority())
                : Priority.MEDIUM;

        Task updated = existing.toBuilder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .completed(newCompleted)
                .dueDate(request.getDueDate() != null && !request.getDueDate().trim().isEmpty() ? request.getDueDate().trim() : null)
                .priority(priority)
                .recurrence(request.getRecurrence() != null && !request.getRecurrence().trim().isEmpty() ? request.getRecurrence().trim() : null)
                .build();

        Task saved = repository.save(updated);

        // Notify updated
        notifyListeners(TaskEventListener.UPDATED, saved);
        // If completed changed from false to true, announce completed
        if (!existing.isCompleted() && saved.isCompleted()) {
            notifyListeners(TaskEventListener.COMPLETED, saved);
        }

        return ApiResponse.ok(saved.toJson());
    }

    public ApiResponse deleteTask(int id) {
        Task existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task with ID " + id + " not found"));

        repository.delete(id);
        notifyListeners(TaskEventListener.DELETED, existing);

        return ApiResponse.ok("{\"message\": \"Task with ID " + id + " and any subtasks deleted successfully\"}");
    }

    // ==========================================
    // AI-Native Task Creation (Step 4 & Step 6)
    // ==========================================

    public ApiResponse createTaskFromText(String text, String idempotencyKey) {
        String fingerprint = null;
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            fingerprint = "createTaskFromText:" + (text != null ? text.trim() : "null");
            Optional<IdempotencyRecord> existing = idempotencyStore.get(idempotencyKey);
            if (existing.isPresent()) {
                if (existing.get().getFingerprint().equals(fingerprint)) {
                    return existing.get().getResponse();
                } else {
                    throw new ConflictException("Idempotency key '" + idempotencyKey + "' was already used with a different request payload");
                }
            }
        }

        if (text == null || text.trim().isEmpty()) {
            throw new BadGatewayException("Input text cannot be blank");
        }

        // Delegate to parser (Strategy Pattern)
        ParsedTask parsed = parser.parse(text, DateValidator.today());
        if (parsed == null) {
            throw new BadGatewayException("Task parser returned null result");
        }

        // Convert to TaskRequest and validate ("never trust the model" rule)
        TaskRequest req = new TaskRequest(
                parsed.getTitle(),
                parsed.getDescription() != null && !parsed.getDescription().isEmpty() ? parsed.getDescription() : parsed.getTitle(),
                "false",
                parsed.getDueDate(),
                parsed.getPriority() != null ? parsed.getPriority() : "MEDIUM",
                parsed.getRecurrence()
        );

        try {
            validateRequest(req);
        } catch (ValidationException ve) {
            // "If validation fails (empty title, impossible date, wrong type), return the error shape with status 502 and save nothing."
            throw new BadGatewayException("LLM generated output failed validation: " + ve.getMessage());
        }

        Priority priority = Priority.fromString(req.getPriority());
        Task task = Task.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .completed(false)
                .dueDate(req.getDueDate())
                .priority(priority)
                .recurrence(req.getRecurrence())
                .createdAt(Instant.now().toString())
                .build();

        Task saved = repository.save(task);
        notifyListeners(TaskEventListener.CREATED, saved);

        ApiResponse response = ApiResponse.created(saved.toJson());
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            idempotencyStore.store(idempotencyKey, fingerprint, response);
        }
        return response;
    }

    // ==========================================
    // Subtasks and Breakdown (Step 5 & Step 6)
    // ==========================================

    public ApiResponse createSubtask(int parentId, TaskRequest request) {
        Task parent = repository.findById(parentId)
                .orElseThrow(() -> new NotFoundException("Parent task with ID " + parentId + " not found"));

        // "A task that is itself a subtask cannot be broken down or given subtasks. Return 400."
        if (parent.isSubtask()) {
            throw new ValidationException("parentId", "A subtask cannot be given subtasks");
        }

        validateRequest(request);

        boolean completed = Boolean.parseBoolean(request.getCompleted().trim());
        Priority priority = request.getPriority() != null && !request.getPriority().trim().isEmpty()
                ? Priority.fromString(request.getPriority())
                : parent.getPriority();

        Task subtask = Task.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .completed(completed)
                .dueDate(request.getDueDate() != null && !request.getDueDate().trim().isEmpty() ? request.getDueDate().trim() : parent.getDueDate())
                .priority(priority)
                .recurrence(request.getRecurrence())
                .parentId(parentId)
                .createdAt(Instant.now().toString())
                .build();

        Task saved = repository.save(subtask);
        notifyListeners(TaskEventListener.SUBTASK_CREATED, saved);

        return ApiResponse.created(saved.toJson());
    }

    public ApiResponse listSubtasks(int parentId) {
        repository.findById(parentId)
                .orElseThrow(() -> new NotFoundException("Parent task with ID " + parentId + " not found"));

        List<Task> subtasks = repository.findByParentId(parentId);
        return ApiResponse.ok(tasksToJson(subtasks));
    }

    public ApiResponse breakDownTask(int id, String idempotencyKey) {
        String fingerprint = null;
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            fingerprint = "breakDownTask:" + id;
            Optional<IdempotencyRecord> existing = idempotencyStore.get(idempotencyKey);
            if (existing.isPresent()) {
                if (existing.get().getFingerprint().equals(fingerprint)) {
                    return existing.get().getResponse();
                } else {
                    throw new ConflictException("Idempotency key '" + idempotencyKey + "' was already used with a different request payload");
                }
            }
        }

        Task parent = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task with ID " + id + " not found"));

        // "A task that is itself a subtask cannot be broken down or given subtasks. Return 400."
        if (parent.isSubtask()) {
            throw new ValidationException("id", "A subtask cannot be broken down");
        }

        // Delegate to BreakdownStrategy
        List<TaskRequest> subtaskRequests;
        try {
            subtaskRequests = breakdownStrategy.breakDown(parent);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new BadGatewayException("Task breakdown failed: " + e.getMessage(), e);
        }

        if (subtaskRequests == null || subtaskRequests.isEmpty()) {
            throw new BadGatewayException("Breakdown produced no subtasks");
        }

        // "Validate every item the model returns first. If any item is invalid, or the model call fails, save none of them and return the error shape (502)."
        for (TaskRequest req : subtaskRequests) {
            try {
                validateRequest(req);
            } catch (ValidationException ve) {
                throw new BadGatewayException("Breakdown generated an invalid subtask item: " + ve.getMessage());
            }
        }

        // All valid: save all subtasks
        List<Task> savedSubtasks = new ArrayList<>();
        for (TaskRequest req : subtaskRequests) {
            boolean completed = Boolean.parseBoolean(req.getCompleted().trim());
            Priority priority = req.getPriority() != null && !req.getPriority().trim().isEmpty()
                    ? Priority.fromString(req.getPriority())
                    : parent.getPriority();

            Task subtask = Task.builder()
                    .title(req.getTitle().trim())
                    .description(req.getDescription().trim())
                    .completed(completed)
                    .dueDate(req.getDueDate() != null && !req.getDueDate().trim().isEmpty() ? req.getDueDate().trim() : parent.getDueDate())
                    .priority(priority)
                    .parentId(id)
                    .createdAt(Instant.now().toString())
                    .build();

            Task saved = repository.save(subtask);
            savedSubtasks.add(saved);
            notifyListeners(TaskEventListener.SUBTASK_CREATED, saved);
        }

        ApiResponse response = ApiResponse.created(tasksToJson(savedSubtasks));
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            idempotencyStore.store(idempotencyKey, fingerprint, response);
        }
        return response;
    }

    // ==========================================
    // Query, Filter, Sort, Pagination (Step 7)
    // ==========================================

    public ApiResponse queryTasks(TaskQuery query) {
        if (query == null) {
            query = TaskQuery.builder().build();
        }

        List<Task> allTasks = repository.findAll();

        // 1. Apply Open/Closed Filters
        final TaskQuery finalQuery = query;
        List<Task> filtered = allTasks.stream()
                .filter(task -> filters.stream().allMatch(f -> f.matches(task, finalQuery)))
                .collect(Collectors.toList());

        // 2. Apply Sort Strategy
        SortStrategy sortStrategy = resolveSortStrategy(query.getSortBy());
        filtered.sort(sortStrategy.getComparator(query.getSortOrder()));

        // 3. Apply Pagination
        int totalTasks = filtered.size();
        int pageSize = query.getPageSize();
        int totalPages = totalTasks == 0 ? 0 : (int) Math.ceil((double) totalTasks / pageSize);
        int page = query.getPage();

        List<Task> pageItems;
        if (page > totalPages || totalTasks == 0) {
            pageItems = Collections.emptyList();
        } else {
            int fromIndex = (page - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, totalTasks);
            pageItems = filtered.subList(fromIndex, toIndex);
        }

        String json = "{\n" +
                "  \"page\": " + page + ",\n" +
                "  \"pageSize\": " + pageSize + ",\n" +
                "  \"totalTasks\": " + totalTasks + ",\n" +
                "  \"totalPages\": " + totalPages + ",\n" +
                "  \"tasks\": " + tasksToJson(pageItems) + "\n" +
                "}";

        return ApiResponse.ok(json);
    }

    private SortStrategy resolveSortStrategy(String sortBy) {
        if (sortBy == null) return new IdSortStrategy();
        switch (sortBy.toLowerCase()) {
            case "duedate":   return new DueDateSortStrategy();
            case "priority":  return new PrioritySortStrategy();
            case "title":     return new TitleSortStrategy();
            case "createdat": return new CreatedAtSortStrategy();
            case "id":
            default:          return new IdSortStrategy();
        }
    }

    private String tasksToJson(List<Task> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            sb.append("  ").append(list.get(i).toJson());
            if (i < list.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
