package app;

import api.ApiResponse;
import api.TaskApi;
import event.AuditTrailListener;
import event.ConsoleLogListener;
import model.Task;
import model.TaskQuery;
import model.TaskRequest;
import repository.InMemoryTaskRepository;
import service.TaskService;

public class Main {

    public static void main(String[] args) {
        System.out.println("Task Manager started");
        System.out.println("================================================================================");
        System.out.println("Running Comprehensive End-to-End Task Manager Showcase");
        System.out.println("================================================================================\n");

        InMemoryTaskRepository repository = InMemoryTaskRepository.getInstance();
        repository.clear();
        TaskService service = new TaskService(repository);
        TaskApi api = new TaskApi(service);

        // =====================================================================
        // STEP 8 DEMO: Setup Observer Pattern Listeners
        // =====================================================================
        System.out.println("--- [STEP 8] Setting Up Observer Listeners ---");
        ConsoleLogListener consoleListener = new ConsoleLogListener();
        AuditTrailListener auditListener = new AuditTrailListener();
        service.addListener(consoleListener);
        service.addListener(auditListener);
        System.out.println("Registered ConsoleLogListener and AuditTrailListener.\n");

        // =====================================================================
        // STEP 2 & 3 DEMO: Task Creation & Validation Error Shape
        // =====================================================================
        System.out.println("--- [STEP 2] Creating a Valid Task ---");
        TaskRequest validRequest = new TaskRequest(
                "Submit quarterly report",
                "Compile Q4 financial metrics and submit to management",
                "false",
                "2026-12-15",
                "HIGH"
        );
        ApiResponse createResponse = api.createTask(validRequest);
        System.out.println("Create Response:\n" + createResponse + "\n");

        System.out.println("--- [STEP 3] Validation Error Demonstration (Multi-issue rejection) ---");
        TaskRequest invalidRequest = new TaskRequest(
                "",                 // empty title -> INVALID
                "",                 // empty description -> INVALID
                "maybe",            // invalid completed -> INVALID
                "2026-02-30",       // impossible date -> INVALID
                "SUPER_HIGH"        // invalid priority -> INVALID
        );
        ApiResponse validationErrorResponse = api.createTask(invalidRequest);
        System.out.println("Validation Error Response (HTTP 400 with strict error shape):\n" + validationErrorResponse + "\n");

        // =====================================================================
        // STEP 2 DEMO: Read, Update, and 404 Not Found
        // =====================================================================
        System.out.println("--- [STEP 2] Get Task by ID (200 OK) ---");
        ApiResponse getResponse = api.getTask(1);
        System.out.println("Get Task 1:\n" + getResponse + "\n");

        System.out.println("--- [STEP 3] Not Found Error (HTTP 404) ---");
        ApiResponse notFoundResponse = api.getTask(999);
        System.out.println("Get Task 999 (404):\n" + notFoundResponse + "\n");

        System.out.println("--- [STEP 2] Update Task (Mark as Completed) ---");
        TaskRequest updateRequest = new TaskRequest(
                "Submit quarterly report",
                "Compiled and submitted Q4 financial metrics",
                "true",
                "2026-12-15",
                "HIGH"
        );
        ApiResponse updateResponse = api.updateTask(1, updateRequest);
        System.out.println("Update Task 1:\n" + updateResponse + "\n");

        // =====================================================================
        // STEP 4 DEMO: AI-Native Task Creation from Text
        // =====================================================================
        System.out.println("--- [STEP 4] AI-Native Task Creation from Text ---");
        String naturalLanguageInput = "Remind me to pay rent on the 1st of every month";
        System.out.println("Input: \"" + naturalLanguageInput + "\"");
        ApiResponse aiCreateResponse = api.createTaskFromText(naturalLanguageInput);
        System.out.println("Parsed & Created Task:\n" + aiCreateResponse + "\n");

        // =====================================================================
        // STEP 5 DEMO: Subtasks, Task Breakdown, and Cascading Delete
        // =====================================================================
        System.out.println("--- [STEP 5] Subtasks and Breakdown ---");
        TaskRequest parentReq = new TaskRequest(
                "Organize Tech Conference",
                "Book venue, invite speakers, and sell tickets",
                "false",
                "2026-11-30",
                "HIGH"
        );
        ApiResponse parentResp = api.createTask(parentReq);
        System.out.println("Created Parent Task:\n" + parentResp + "\n");

        // Manual Subtask creation
        TaskRequest subtaskReq = new TaskRequest(
                "Book auditorium",
                "Finalize contract with convention center",
                "false",
                "2026-11-20",
                "HIGH"
        );
        ApiResponse subtaskResp = api.createSubtask(3, subtaskReq);
        System.out.println("Created Manual Subtask:\n" + subtaskResp + "\n");

        // Automated Task Breakdown
        System.out.println("Breaking down parent task #3 into subtasks:");
        ApiResponse breakdownResp = api.breakDownTask(3);
        System.out.println("Breakdown Output:\n" + breakdownResp + "\n");

        // Verify subtask list
        ApiResponse subtasksList = api.listSubtasks(3);
        System.out.println("List Subtasks of Task #3:\n" + subtasksList + "\n");

        // Subtask of a subtask rejection (HTTP 400)
        System.out.println("Attempting to create a subtask under an existing subtask (Rule: Reject with 400):");
        TaskRequest illegalSubtask = new TaskRequest("Sub-subtask", "Should fail", "false", null, "LOW");
        ApiResponse illegalSubtaskResp = api.createSubtask(4, illegalSubtask);
        System.out.println("Illegal Subtask Response:\n" + illegalSubtaskResp + "\n");

        // Cascading Deletion Demonstration
        System.out.println("--- Cascading Delete Verification ---");
        System.out.println("Repository contents BEFORE deleting parent task #3:");
        for (Task t : repository.findAll()) {
            System.out.println("  -> ID=" + t.getId() + ", Title='" + t.getTitle() + "', ParentID=" + t.getParentId());
        }
        System.out.println("\nDeleting parent task #3...");
        api.deleteTask(3);
        System.out.println("\nRepository contents AFTER deleting parent task #3 (Parent and subtasks deleted):");
        for (Task t : repository.findAll()) {
            System.out.println("  -> ID=" + t.getId() + ", Title='" + t.getTitle() + "', ParentID=" + t.getParentId());
        }
        System.out.println();

        // =====================================================================
        // STEP 6 DEMO: Idempotency
        // =====================================================================
        System.out.println("--- [STEP 6] Safe Retries (Idempotency) Demonstration ---");
        String idempotencyKey = "client-tx-9901";
        TaskRequest idempotentReq = new TaskRequest("Idempotent Task", "Testing duplicate safety", "false", "2026-12-25", "MEDIUM");

        System.out.println("Call 1: Creating task with key: " + idempotencyKey);
        ApiResponse firstCall = api.createTask(idempotentReq, idempotencyKey);
        System.out.println("First call response code: " + firstCall.getStatusCode());

        System.out.println("Call 2: Retrying same request with SAME key: " + idempotencyKey);
        ApiResponse secondCall = api.createTask(idempotentReq, idempotencyKey);
        System.out.println("Second call response code: " + secondCall.getStatusCode());
        System.out.println("Responses identical? " + firstCall.getBody().equals(secondCall.getBody()));

        System.out.println("Verifying repository: Checking how many tasks with title 'Idempotent Task' exist:");
        long count = repository.findAll().stream().filter(t -> t.getTitle().equals("Idempotent Task")).count();
        System.out.println("Count in repository: " + count + " (MUST BE 1)\n");

        System.out.println("Call 3: Using SAME key but DIFFERENT payload (Rule: Reject with 422 Conflict):");
        TaskRequest differentPayloadReq = new TaskRequest("Changed Title", "Different content", "false", null, "LOW");
        ApiResponse conflictResp = api.createTask(differentPayloadReq, idempotencyKey);
        System.out.println("Conflict Response:\n" + conflictResp + "\n");

        // =====================================================================
        // STEP 7 DEMO: Filtering, Search, Sorting, and Pagination
        // =====================================================================
        System.out.println("--- [STEP 7] Filtering, Search, Sorting & Pagination Combined ---");
        // Seed several tasks
        api.createTask(new TaskRequest("Alpha Review documentation", "Review chapter 1", "false", "2026-12-05", "LOW"));
        api.createTask(new TaskRequest("Beta Fix security issue", "Review authentication vulnerabilities", "false", "2026-12-01", "HIGH"));
        api.createTask(new TaskRequest("Gamma Review pull request", "Review PR #42", "true", "2026-12-03", "MEDIUM"));
        api.createTask(new TaskRequest("Delta Review test cases", "Review edge cases", "false", "2026-12-10", "HIGH"));

        System.out.println("Query Criteria:");
        System.out.println(" - searchKeyword: 'Review'");
        System.out.println(" - completed: false");
        System.out.println(" - sortBy: 'dueDate', sortOrder: 'ASC'");
        System.out.println(" - page: 1, pageSize: 2");

        TaskQuery query = TaskQuery.builder()
                .searchKeyword("Review")
                .completed(false)
                .sortBy("dueDate")
                .sortOrder("ASC")
                .page(1)
                .pageSize(2)
                .build();

        ApiResponse queryResponse = api.queryTasks(query);
        System.out.println("Query Result:\n" + queryResponse + "\n");

        // =====================================================================
        // STEP 8 DEMO: Observer Verification & Listener Removal
        // =====================================================================
        System.out.println("--- [STEP 8] Observer Listener Audit & Removal Verification ---");
        System.out.println("Audit trail summary:");
        System.out.println(" - TASK_CREATED events: " + auditListener.getCount("TASK_CREATED"));
        System.out.println(" - TASK_UPDATED events: " + auditListener.getCount("TASK_UPDATED"));
        System.out.println(" - TASK_COMPLETED events: " + auditListener.getCount("TASK_COMPLETED"));
        System.out.println(" - TASK_DELETED events: " + auditListener.getCount("TASK_DELETED"));
        System.out.println(" - SUBTASK_CREATED events: " + auditListener.getCount("SUBTASK_CREATED"));
        System.out.println(" - Total events tracked: " + auditListener.getTotalCount());

        System.out.println("\nRemoving ConsoleLogListener...");
        service.removeListener(consoleListener);
        System.out.println("Creating another task (ConsoleLogListener should NOT print anything below):");
        api.createTask(new TaskRequest("Silent Task", "Console listener should not log this", "false", null, "LOW"));
        System.out.println("Audit trail count after silent task: " + auditListener.getTotalCount());
        System.out.println("Done! Listener removal verified.\n");

        System.out.println("================================================================================");
        System.out.println("Main demonstration completed successfully!");
        System.out.println("================================================================================");
    }
}
