package app;

import api.ApiResponse;
import api.TaskApi;
import event.AuditTrailListener;
import event.ConsoleLogListener;
import event.TaskEventListener;
import exception.ServiceUnavailableException;
import model.Priority;
import model.Task;
import model.TaskQuery;
import model.TaskRequest;
import parser.DateValidator;
import parser.FakeLlmClient;
import parser.FaultyLlmClient;
import parser.LlmTaskParser;
import parser.ParserFactory;
import parser.RuleBasedTaskParser;
import parser.SimpleJson;
import parser.TaskParser;
import repository.InMemoryTaskRepository;
import service.BreakdownStrategyFactory;
import service.IdempotencyStore;
import service.LlmBreakdownStrategy;
import service.RuleBasedBreakdownStrategy;
import service.TaskService;

import java.util.List;
import java.util.Map;

/**
 * Autonomous test runner verifying all 10 requirements and edge cases.
 * Runs completely offline without external libraries or live API keys.
 */
public class TestRunner {

    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("Starting Task Manager Test Suite (Plain Java, Zero Dependencies)");
        System.out.println("================================================================================\n");

        runAllTests();

        System.out.println("\n================================================================================");
        System.out.println("Test Suite Execution Summary:");
        System.out.println("Passed: " + passedCount);
        System.out.println("Failed: " + failedCount);
        System.out.println("Total:  " + (passedCount + failedCount));
        System.out.println("================================================================================");

        if (failedCount > 0) {
            System.exit(1);
        }
    }

    private static void resetEnvironment() {
        InMemoryTaskRepository.getInstance().clear();
        IdempotencyStore.getInstance().clear();
    }

    private static void runTest(String testName, Runnable test) {
        resetEnvironment();
        try {
            test.run();
            passedCount++;
            System.out.println("[PASS] " + testName);
        } catch (Throwable t) {
            failedCount++;
            System.err.println("[FAIL] " + testName + " -> " + t.getMessage());
            t.printStackTrace(System.err);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError("Expected [" + expected + "] but found [" + actual + "]: " + message);
        }
    }

    private static void runAllTests() {
        // --- Core CRUD & Validation ---
        runTest("Create Task Success (201 Created)", () -> {
            TaskApi api = new TaskApi();
            TaskRequest req = new TaskRequest("Buy groceries", "Milk, butter, bread", "false", "2026-12-01", "MEDIUM");
            ApiResponse res = api.createTask(req);
            assertEquals(201, res.getStatusCode(), "Status should be 201");
            assertTrue(res.getBody().contains("\"title\":\"Buy groceries\""), "Body should contain title");
        });

        runTest("Validation: Empty Title Rejected (400)", () -> {
            TaskApi api = new TaskApi();
            TaskRequest req = new TaskRequest("", "Valid description", "false", "2026-12-01", "LOW");
            ApiResponse res = api.createTask(req);
            assertEquals(400, res.getStatusCode(), "Status should be 400");
            assertTrue(res.getBody().contains("VALIDATION_ERROR"), "Error code should be VALIDATION_ERROR");
            assertTrue(res.getBody().contains("\"field\":\"title\""), "Details must cite title");
        });

        runTest("Validation: Empty Description Rejected (400)", () -> {
            TaskApi api = new TaskApi();
            TaskRequest req = new TaskRequest("Valid title", "", "false", "2026-12-01", "LOW");
            ApiResponse res = api.createTask(req);
            assertEquals(400, res.getStatusCode(), "Status should be 400");
            assertTrue(res.getBody().contains("\"field\":\"description\""), "Details must cite description");
        });

        runTest("Validation: Invalid Completed Value Rejected (400)", () -> {
            TaskApi api = new TaskApi();
            TaskRequest req = new TaskRequest("Valid title", "Valid desc", "yes", "2026-12-01", "LOW");
            ApiResponse res = api.createTask(req);
            assertEquals(400, res.getStatusCode(), "Status should be 400");
            assertTrue(res.getBody().contains("\"field\":\"completed\""), "Details must cite completed");
        });

        runTest("Validation: Impossible Date Rejected (2026-02-30) (400)", () -> {
            TaskApi api = new TaskApi();
            TaskRequest req = new TaskRequest("Valid title", "Valid desc", "false", "2026-02-30", "LOW");
            ApiResponse res = api.createTask(req);
            assertEquals(400, res.getStatusCode(), "Status should be 400");
            assertTrue(res.getBody().contains("\"field\":\"dueDate\""), "Details must cite dueDate");
        });

        runTest("Validation: Non-Leap Year Feb 29 Rejected (2023-02-29) (400)", () -> {
            TaskApi api = new TaskApi();
            TaskRequest req = new TaskRequest("Valid title", "Valid desc", "false", "2023-02-29", "LOW");
            ApiResponse res = api.createTask(req);
            assertEquals(400, res.getStatusCode(), "Status should be 400");
            assertTrue(res.getBody().contains("\"field\":\"dueDate\""), "Details must cite dueDate");
        });

        runTest("Validation: Leap Year Feb 29 Accepted (2024-02-29) (201)", () -> {
            TaskApi api = new TaskApi();
            TaskRequest req = new TaskRequest("Leap task", "Valid leap day", "false", "2024-02-29", "MEDIUM");
            ApiResponse res = api.createTask(req);
            assertEquals(201, res.getStatusCode(), "Status should be 201 for valid leap year date");
        });

        runTest("Validation: Invalid Priority String Rejected (400)", () -> {
            TaskApi api = new TaskApi();
            TaskRequest req = new TaskRequest("Valid title", "Valid desc", "false", "2026-12-01", "CRITICAL_NOW");
            ApiResponse res = api.createTask(req);
            assertEquals(400, res.getStatusCode(), "Status should be 400");
            assertTrue(res.getBody().contains("\"field\":\"priority\""), "Details must cite priority");
        });

        runTest("Validation: Multi-issue Rejection Reports All Violations (400)", () -> {
            TaskApi api = new TaskApi();
            TaskRequest req = new TaskRequest("", "", "not_bool", "2026-02-30", "INVALID_PRIORITY");
            ApiResponse res = api.createTask(req);
            assertEquals(400, res.getStatusCode(), "Status should be 400");
            assertTrue(res.getBody().contains("\"field\":\"title\""), "Must contain title issue");
            assertTrue(res.getBody().contains("\"field\":\"description\""), "Must contain description issue");
            assertTrue(res.getBody().contains("\"field\":\"completed\""), "Must contain completed issue");
            assertTrue(res.getBody().contains("\"field\":\"dueDate\""), "Must contain dueDate issue");
            assertTrue(res.getBody().contains("\"field\":\"priority\""), "Must contain priority issue");
        });

        runTest("Read Task: Found (200 OK)", () -> {
            TaskApi api = new TaskApi();
            ApiResponse created = api.createTask(new TaskRequest("Task 1", "Desc 1", "false", null, "LOW"));
            int id = Integer.parseInt(SimpleJson.getString(created.getBody(), "id"));

            ApiResponse res = api.getTask(id);
            assertEquals(200, res.getStatusCode(), "Status should be 200");
            assertTrue(res.getBody().contains("Task 1"), "Body should contain task title");
        });

        runTest("Read Task: Not Found (404 NOT_FOUND)", () -> {
            TaskApi api = new TaskApi();
            ApiResponse res = api.getTask(8888);
            assertEquals(404, res.getStatusCode(), "Status should be 404");
            assertTrue(res.getBody().contains("NOT_FOUND"), "Error code should be NOT_FOUND");
        });

        runTest("Update Task Success (200 OK)", () -> {
            TaskApi api = new TaskApi();
            ApiResponse created = api.createTask(new TaskRequest("Old Title", "Old Desc", "false", null, "LOW"));
            int id = Integer.parseInt(SimpleJson.getString(created.getBody(), "id"));

            TaskRequest updateReq = new TaskRequest("New Title", "New Desc", "true", "2026-12-31", "HIGH");
            ApiResponse updated = api.updateTask(id, updateReq);
            assertEquals(200, updated.getStatusCode(), "Status should be 200");
            assertTrue(updated.getBody().contains("\"title\":\"New Title\""), "Updated title should match");
            assertTrue(updated.getBody().contains("\"completed\":true"), "Completed should be true");
        });

        runTest("Update Task: Not Found (404 NOT_FOUND)", () -> {
            TaskApi api = new TaskApi();
            ApiResponse res = api.updateTask(7777, new TaskRequest("T", "D", "false", null, "LOW"));
            assertEquals(404, res.getStatusCode(), "Status should be 404");
            assertTrue(res.getBody().contains("NOT_FOUND"), "Error code should be NOT_FOUND");
        });

        runTest("Delete Task: Not Found (404 NOT_FOUND)", () -> {
            TaskApi api = new TaskApi();
            ApiResponse res = api.deleteTask(9999);
            assertEquals(404, res.getStatusCode(), "Status should be 404");
            assertTrue(res.getBody().contains("NOT_FOUND"), "Error code should be NOT_FOUND");
        });

        runTest("Subtask Operations with Unknown Parent Return 404", () -> {
            TaskApi api = new TaskApi();
            ApiResponse createSub = api.createSubtask(9999, new TaskRequest("Sub", "Desc", "false", null, "LOW"));
            assertEquals(404, createSub.getStatusCode(), "Create subtask for unknown parent returns 404");

            ApiResponse listSub = api.listSubtasks(9999);
            assertEquals(404, listSub.getStatusCode(), "List subtasks for unknown parent returns 404");

            ApiResponse breakSub = api.breakDownTask(9999);
            assertEquals(404, breakSub.getStatusCode(), "Breakdown for unknown parent returns 404");
        });

        // --- Subtasks and Cascading Deletion ---
        runTest("Create Subtask Success (201 Created)", () -> {
            TaskApi api = new TaskApi();
            ApiResponse parent = api.createTask(new TaskRequest("Parent Task", "Parent Desc", "false", null, "MEDIUM"));
            int parentId = Integer.parseInt(SimpleJson.getString(parent.getBody(), "id"));

            TaskRequest subReq = new TaskRequest("Subtask 1", "Subtask Desc", "false", null, "LOW");
            ApiResponse subRes = api.createSubtask(parentId, subReq);
            assertEquals(201, subRes.getStatusCode(), "Status should be 201");
            assertTrue(subRes.getBody().contains("\"parentId\":" + parentId), "Parent ID should match");
        });

        runTest("Subtask of Subtask Rejected (400 VALIDATION_ERROR)", () -> {
            TaskApi api = new TaskApi();
            ApiResponse parent = api.createTask(new TaskRequest("Root Task", "Root Desc", "false", null, "MEDIUM"));
            int rootId = Integer.parseInt(SimpleJson.getString(parent.getBody(), "id"));

            ApiResponse subtask = api.createSubtask(rootId, new TaskRequest("Child Subtask", "Child Desc", "false", null, "LOW"));
            int childId = Integer.parseInt(SimpleJson.getString(subtask.getBody(), "id"));

            // Attempt sub-subtask
            ApiResponse subSub = api.createSubtask(childId, new TaskRequest("Sub Subtask", "Invalid depth", "false", null, "LOW"));
            assertEquals(400, subSub.getStatusCode(), "Subtask of subtask must be rejected with 400");
            assertTrue(subSub.getBody().contains("A subtask cannot be given subtasks"), "Error must state rule");
        });

        runTest("Breakdown of Subtask Rejected (400 VALIDATION_ERROR)", () -> {
            TaskApi api = new TaskApi();
            ApiResponse parent = api.createTask(new TaskRequest("Root Task", "Root Desc", "false", null, "MEDIUM"));
            int rootId = Integer.parseInt(SimpleJson.getString(parent.getBody(), "id"));

            ApiResponse subtask = api.createSubtask(rootId, new TaskRequest("Child Subtask", "Child Desc", "false", null, "LOW"));
            int childId = Integer.parseInt(SimpleJson.getString(subtask.getBody(), "id"));

            ApiResponse breakdownRes = api.breakDownTask(childId);
            assertEquals(400, breakdownRes.getStatusCode(), "Breaking down subtask must return 400");
            assertTrue(breakdownRes.getBody().contains("A subtask cannot be broken down"), "Error must state rule");
        });

        runTest("Cascading Delete Removes Parent and All Subtasks", () -> {
            TaskApi api = new TaskApi();
            InMemoryTaskRepository repo = InMemoryTaskRepository.getInstance();

            ApiResponse parent = api.createTask(new TaskRequest("Parent To Delete", "Desc", "false", null, "MEDIUM"));
            int parentId = Integer.parseInt(SimpleJson.getString(parent.getBody(), "id"));

            api.createSubtask(parentId, new TaskRequest("Sub 1", "Desc", "false", null, "LOW"));
            api.createSubtask(parentId, new TaskRequest("Sub 2", "Desc", "false", null, "LOW"));

            assertEquals(3, repo.findAll().size(), "Should have 3 tasks total before delete");

            api.deleteTask(parentId);

            assertEquals(0, repo.findAll().size(), "Repository should have 0 tasks after cascading parent delete");
        });

        // --- Idempotency (Step 6) ---
        runTest("Idempotency: Replaying Same Key and Request Returns Cached Response (No Duplicates)", () -> {
            TaskApi api = new TaskApi();
            InMemoryTaskRepository repo = InMemoryTaskRepository.getInstance();

            String key = "test-key-001";
            TaskRequest req = new TaskRequest("Pay electric bill", "Due in 2 days", "false", "2026-12-05", "HIGH");

            ApiResponse first = api.createTask(req, key);
            assertEquals(201, first.getStatusCode(), "First call should create task (201)");

            ApiResponse second = api.createTask(req, key);
            assertEquals(201, second.getStatusCode(), "Second call should return cached response");
            assertEquals(first.getBody(), second.getBody(), "Responses must be identical");

            assertEquals(1, repo.findAll().size(), "Repository must contain exactly ONE task, not a duplicate");
        });

        runTest("Idempotency: Reusing Same Key with Different Request Returns 422 Conflict", () -> {
            TaskApi api = new TaskApi();
            String key = "test-key-002";
            TaskRequest req1 = new TaskRequest("Original Title", "Desc", "false", null, "LOW");
            TaskRequest req2 = new TaskRequest("Altered Title", "Desc", "false", null, "LOW");

            api.createTask(req1, key);
            ApiResponse conflict = api.createTask(req2, key);

            assertEquals(422, conflict.getStatusCode(), "Conflict status must be 422");
            assertTrue(conflict.getBody().contains("CONFLICT"), "Error code must be CONFLICT");
        });

        runTest("Idempotency: Failed Requests Are NOT Stored (Client Can Fix and Retry)", () -> {
            TaskApi api = new TaskApi();
            String key = "test-key-003";

            // Invalid request (empty title)
            TaskRequest badReq = new TaskRequest("", "Desc", "false", null, "LOW");
            ApiResponse err = api.createTask(badReq, key);
            assertEquals(400, err.getStatusCode(), "Bad request gives 400");

            // Fixed request with same key
            TaskRequest fixedReq = new TaskRequest("Fixed Title", "Desc", "false", null, "LOW");
            ApiResponse success = api.createTask(fixedReq, key);
            assertEquals(201, success.getStatusCode(), "Should successfully create task on retry");
        });

        // --- Filtering, Sorting, and Pagination (Step 7) ---
        runTest("Filtering and Pagination: Query Returns Matching Slices and Metadata", () -> {
            TaskApi api = new TaskApi();
            api.createTask(new TaskRequest("Deploy Backend", "Spring / Java", "false", "2026-12-01", "HIGH"));
            api.createTask(new TaskRequest("Deploy Frontend", "React app", "false", "2026-12-02", "MEDIUM"));
            api.createTask(new TaskRequest("Deploy Database", "PostgreSQL", "true", "2026-12-03", "HIGH"));
            api.createTask(new TaskRequest("Write Docs", "Swagger", "false", "2026-12-04", "LOW"));

            // Filter by completed=false, search="Deploy"
            TaskQuery query = TaskQuery.builder()
                    .completed(false)
                    .searchKeyword("Deploy")
                    .sortBy("dueDate")
                    .sortOrder("ASC")
                    .page(1)
                    .pageSize(1)
                    .build();

            ApiResponse res = api.queryTasks(query);
            assertEquals(200, res.getStatusCode(), "Query status must be 200");
            assertTrue(res.getBody().contains("\"totalTasks\": 2"), "Should match 2 uncompleted Deploy tasks");
            assertTrue(res.getBody().contains("\"totalPages\": 2"), "Total pages should be 2");
            assertTrue(res.getBody().contains("Deploy Backend"), "First page item should be Deploy Backend");
        });

        runTest("Pagination: Requesting Page Number Beyond Total Pages Returns Empty List (No Crash)", () -> {
            TaskApi api = new TaskApi();
            api.createTask(new TaskRequest("Single Task", "Desc", "false", null, "LOW"));

            TaskQuery query = TaskQuery.builder()
                    .page(99)
                    .pageSize(10)
                    .build();

            ApiResponse res = api.queryTasks(query);
            assertEquals(200, res.getStatusCode(), "Status should be 200");
            assertTrue(res.getBody().contains("\"tasks\": []"), "Tasks list should be empty");
            assertTrue(res.getBody().contains("\"page\": 99"), "Page should reflect 99");
        });

        // --- Observer Pattern (Step 8) ---
        runTest("Observer Pattern: Notifies Listeners and Supports Removal", () -> {
            InMemoryTaskRepository repo = InMemoryTaskRepository.getInstance();
            TaskService service = new TaskService(repo);
            TaskApi api = new TaskApi(service);

            AuditTrailListener audit = new AuditTrailListener();
            service.addListener(audit);

            ApiResponse created = api.createTask(new TaskRequest("Audited Task", "Desc", "false", null, "LOW"));
            int id = Integer.parseInt(SimpleJson.getString(created.getBody(), "id"));

            assertEquals(1, audit.getCount("TASK_CREATED"), "Should record 1 CREATED event");

            api.updateTask(id, new TaskRequest("Audited Task", "Desc", "true", null, "LOW"));
            assertEquals(1, audit.getCount("TASK_UPDATED"), "Should record 1 UPDATED event");
            assertEquals(1, audit.getCount("TASK_COMPLETED"), "Should record 1 COMPLETED event");

            // Remove listener
            service.removeListener(audit);
            api.createTask(new TaskRequest("Silent Task", "Desc", "false", null, "LOW"));

            // Count should remain unchanged after removal
            assertEquals(1, audit.getCount("TASK_CREATED"), "Count should not increase after listener removed");
        });

        // --- Fake LLM and Faulty LLM (Step 9) ---
        runTest("Fake LLM Client: Generates Structured Task Successfully (201 Created)", () -> {
            String fakeLlmJson = "{\n" +
                    "  \"title\": \"Pay Rent\",\n" +
                    "  \"description\": \"Pay apartment rent for next month\",\n" +
                    "  \"dueDate\": \"2026-12-01\",\n" +
                    "  \"recurrence\": \"1st of every month\",\n" +
                    "  \"priority\": \"HIGH\"\n" +
                    "}";
            FakeLlmClient fakeLlm = new FakeLlmClient(fakeLlmJson);
            TaskParser parser = new LlmTaskParser(fakeLlm);
            TaskService service = new TaskService(InMemoryTaskRepository.getInstance(), parser, BreakdownStrategyFactory.createDefaultStrategy(), IdempotencyStore.getInstance());
            TaskApi api = new TaskApi(service);

            ApiResponse res = api.createTaskFromText("Remind me to pay rent on the 1st of every month");
            assertEquals(201, res.getStatusCode(), "Status should be 201");
            assertTrue(res.getBody().contains("\"title\":\"Pay Rent\""), "Title should match model output");
            assertEquals(1, fakeLlm.getCallCount(), "Model should be called once");
        });

        runTest("Faulty LLM Client: Network Timeout Throws 502 Bad Gateway and Saves Nothing", () -> {
            FaultyLlmClient faultyLlm = new FaultyLlmClient(FaultyLlmClient.FaultMode.THROW_NETWORK_EXCEPTION);
            TaskParser parser = new LlmTaskParser(faultyLlm);
            TaskService service = new TaskService(InMemoryTaskRepository.getInstance(), parser, BreakdownStrategyFactory.createDefaultStrategy(), IdempotencyStore.getInstance());
            TaskApi api = new TaskApi(service);

            ApiResponse res = api.createTaskFromText("Schedule dentist visit");
            assertEquals(502, res.getStatusCode(), "Status must be 502 Bad Gateway");
            assertTrue(res.getBody().contains("BAD_GATEWAY"), "Error code must be BAD_GATEWAY");
            assertEquals(0, InMemoryTaskRepository.getInstance().findAll().size(), "Zero tasks should be saved on failure");
        });

        runTest("Faulty LLM Client: Malformed Non-JSON Reply Throws 502 Bad Gateway", () -> {
            FaultyLlmClient faultyLlm = new FaultyLlmClient(FaultyLlmClient.FaultMode.RETURN_MALFORMED_JSON);
            TaskParser parser = new LlmTaskParser(faultyLlm);
            TaskService service = new TaskService(InMemoryTaskRepository.getInstance(), parser, BreakdownStrategyFactory.createDefaultStrategy(), IdempotencyStore.getInstance());
            TaskApi api = new TaskApi(service);

            ApiResponse res = api.createTaskFromText("Schedule dentist visit");
            assertEquals(502, res.getStatusCode(), "Status must be 502");
            assertTrue(res.getBody().contains("BAD_GATEWAY"), "Error code must be BAD_GATEWAY");
            assertEquals(0, InMemoryTaskRepository.getInstance().findAll().size(), "Zero tasks saved");
        });

        runTest("Faulty LLM Client: Model Returns Empty Title Throws 502 (Never Trust Model Rule)", () -> {
            FaultyLlmClient faultyLlm = new FaultyLlmClient(FaultyLlmClient.FaultMode.RETURN_EMPTY_TITLE);
            TaskParser parser = new LlmTaskParser(faultyLlm);
            TaskService service = new TaskService(InMemoryTaskRepository.getInstance(), parser, BreakdownStrategyFactory.createDefaultStrategy(), IdempotencyStore.getInstance());
            TaskApi api = new TaskApi(service);

            ApiResponse res = api.createTaskFromText("Do something");
            assertEquals(502, res.getStatusCode(), "Status must be 502 when model produces invalid domain data");
            assertTrue(res.getBody().contains("BAD_GATEWAY"), "Error code must be BAD_GATEWAY");
            assertEquals(0, InMemoryTaskRepository.getInstance().findAll().size(), "Zero tasks saved");
        });

        runTest("Faulty LLM Client: Model Returns Impossible Date (2026-02-30) Throws 502", () -> {
            FaultyLlmClient faultyLlm = new FaultyLlmClient(FaultyLlmClient.FaultMode.RETURN_IMPOSSIBLE_DATE);
            TaskParser parser = new LlmTaskParser(faultyLlm);
            TaskService service = new TaskService(InMemoryTaskRepository.getInstance(), parser, BreakdownStrategyFactory.createDefaultStrategy(), IdempotencyStore.getInstance());
            TaskApi api = new TaskApi(service);

            ApiResponse res = api.createTaskFromText("Pay bill on leap day");
            assertEquals(502, res.getStatusCode(), "Status must be 502 on impossible date from model");
            assertTrue(res.getBody().contains("BAD_GATEWAY"), "Error code must be BAD_GATEWAY");
            assertEquals(0, InMemoryTaskRepository.getInstance().findAll().size(), "Zero tasks saved");
        });

        runTest("Faulty LLM Breakdown: Invalid Subtask Line Throws 502 and Saves None", () -> {
            FakeLlmClient badBreakdownClient = new FakeLlmClient("Valid Subtask 1|Desc 1\nInvalidLineWithoutDelimiter\nValid Subtask 2|Desc 2");
            LlmBreakdownStrategy strategy = new LlmBreakdownStrategy(badBreakdownClient);
            TaskService service = new TaskService(InMemoryTaskRepository.getInstance(), new RuleBasedTaskParser(), strategy, IdempotencyStore.getInstance());
            TaskApi api = new TaskApi(service);

            ApiResponse parent = api.createTask(new TaskRequest("Master Project", "Big task", "false", null, "HIGH"));
            int parentId = Integer.parseInt(SimpleJson.getString(parent.getBody(), "id"));

            ApiResponse res = api.breakDownTask(parentId);
            assertEquals(502, res.getStatusCode(), "Status must be 502 on faulty breakdown");
            // Only the parent task should exist in repo; no subtasks saved
            assertEquals(1, InMemoryTaskRepository.getInstance().findAll().size(), "No partial subtasks saved");
        });

        runTest("Idempotency with LLM Parser: Repeated Key NEVER Calls LLM Twice", () -> {
            String fakeLlmJson = "{\"title\": \"Idempotent AI Task\", \"description\": \"Desc\", \"dueDate\": null, \"priority\": \"LOW\"}";
            FakeLlmClient fakeLlm = new FakeLlmClient(fakeLlmJson);
            TaskParser parser = new LlmTaskParser(fakeLlm);
            TaskService service = new TaskService(InMemoryTaskRepository.getInstance(), parser, BreakdownStrategyFactory.createDefaultStrategy(), IdempotencyStore.getInstance());
            TaskApi api = new TaskApi(service);

            String key = "ai-idem-key-888";
            ApiResponse call1 = api.createTaskFromText("Make dinner reservation", key);
            assertEquals(201, call1.getStatusCode(), "Call 1 should succeed (201)");
            assertEquals(1, fakeLlm.getCallCount(), "LLM should be called once");

            ApiResponse call2 = api.createTaskFromText("Make dinner reservation", key);
            assertEquals(201, call2.getStatusCode(), "Call 2 should return cached response (201)");
            assertEquals(1, fakeLlm.getCallCount(), "LLM call count MUST remain 1 (never called twice)");
            assertEquals(1, InMemoryTaskRepository.getInstance().findAll().size(), "Only one task in repository");
        });

        runTest("Service Unavailable (503) When API Key Missing for LLM Factory", () -> {
            // Test that when ParserFactory.createLlmParser is called without key, it throws 503
            boolean threw503 = false;
            try {
                // Temporarily ensure LLM_API_KEY is not read as valid if null/empty
                if (System.getenv("LLM_API_KEY") == null || System.getenv("LLM_API_KEY").trim().isEmpty()) {
                    ParserFactory.createLlmParser();
                } else {
                    threw503 = true; // Environment has key, skip
                }
            } catch (ServiceUnavailableException e) {
                assertEquals(503, e.getStatusCode(), "Exception status code should be 503");
                assertEquals("SERVICE_UNAVAILABLE", e.getErrorCode(), "Error code should be SERVICE_UNAVAILABLE");
                threw503 = true;
            }
            assertTrue(threw503, "ServiceUnavailableException must be thrown when API key is missing");
        });
    }
}
