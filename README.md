# AI-Native Task Manager (Plain Java)

A lightweight, enterprise-grade Task Manager built in pure Java 17 with **zero external libraries or frameworks**. It simulates a RESTful API architecture while integrating LLM intelligence to transform natural language into validated tasks, deconstruct complex tasks into subtasks, enforce strict idempotency, execute Open/Closed filters and sorts, and dispatch reactive domain events.

---

## 1. Project Overview & Package Structure

The architecture enforces strict separation of concerns, Single Responsibility, and the Dependency Inversion Principle. The service operations act as framework-free API controllers returning uniform `ApiResponse` objects with HTTP status codes and JSON payloads.

```
task-manager/
├── src/
│   ├── api/                  # API Gateway layer & error response factories
│   │   ├── ApiResponse.java
│   │   ├── ErrorResponseFactory.java
│   │   └── TaskApi.java
│   ├── app/                  # Application entry points & test runners
│   │   ├── Main.java
│   │   └── TestRunner.java
│   ├── event/                # Observer pattern event contracts & listeners
│   │   ├── AuditTrailListener.java
│   │   ├── ConsoleLogListener.java
│   │   └── TaskEventListener.java
│   ├── exception/            # Custom API exception hierarchy
│   │   ├── ApiException.java
│   │   ├── BadGatewayException.java
│   │   ├── ConflictException.java
│   │   ├── FieldIssue.java
│   │   ├── NotFoundException.java
│   │   ├── ServiceUnavailableException.java
│   │   └── ValidationException.java
│   ├── model/                # Immutable domain models, DTOs & queries
│   │   ├── ParsedTask.java
│   │   ├── Priority.java
│   │   ├── Task.java
│   │   ├── TaskQuery.java
│   │   └── TaskRequest.java
│   ├── parser/               # AI & rule-based text parsing strategies
│   │   ├── DateValidator.java
│   │   ├── FakeLlmClient.java
│   │   ├── FaultyLlmClient.java
│   │   ├── HttpLlmClient.java
│   │   ├── LlmClient.java
│   │   ├── LlmTaskParser.java
│   │   ├── ParserFactory.java
│   │   ├── RuleBasedTaskParser.java
│   │   ├── SimpleJson.java
│   │   └── TaskParser.java
│   ├── repository/           # Data access interfaces & in-memory stores
│   │   ├── InMemoryTaskRepository.java
│   │   └── TaskRepository.java
│   └── service/              # Business logic, strategies & idempotency store
│       ├── BreakdownStrategy.java
│       ├── BreakdownStrategyFactory.java
│       ├── CreatedAtSortStrategy.java
│       ├── DueDateFilter.java
│       ├── DueDateSortStrategy.java
│       ├── IdSortStrategy.java
│       ├── IdempotencyRecord.java
│       ├── IdempotencyStore.java
│       ├── LlmBreakdownStrategy.java
│       ├── ParentIdFilter.java
│       ├── PriorityFilter.java
│       ├── PrioritySortStrategy.java
│       ├── RuleBasedBreakdownStrategy.java
│       ├── SearchFilter.java
│       ├── SortStrategy.java
│       ├── StatusFilter.java
│       ├── TaskFilter.java
│       ├── TaskService.java
│       └── TitleSortStrategy.java
├── API_CONTRACT.md           # Formal REST-like API specification
├── compile.sh                # Compilation helper script
├── run.sh                    # Script to execute Main showcase
├── test.sh                   # Script to execute TestRunner suite
└── README.md
```

---

## 2. Setup Instructions

### Prerequisites
- **JDK 17** or newer (OpenJDK, Zulu 17, Eclipse Temurin, etc.).
- No Maven, Gradle, or third-party JARs are needed. Plain `javac` and `java` are used.

### Compilation
Compile all source files into the `out/` directory:
```bash
./compile.sh
```
*Or manually:*
```bash
mkdir -p out
javac -d out $(find src -name "*.java")
```

### Running the End-to-End Showcase (`Main`)
Run the comprehensive CLI showcase demonstrating all operations, validation errors, cascading deletions, idempotency replays, and observer reactions:
```bash
./run.sh
```
*Or manually:*
```bash
java -cp out app.Main
```

### Running the Automated Test Suite (`TestRunner`)
Run the 33 unit and integration tests (executes 100% offline without network calls or API keys):
```bash
./test.sh
```
*Or manually:*
```bash
java -cp out app.TestRunner
```

### Configuring `LLM_API_KEY` (Optional for Live LLM Mode)
The application works offline out-of-the-box using the built-in `RuleBasedTaskParser` and `RuleBasedBreakdownStrategy`. If you wish to connect to a live LLM (Gemini or OpenAI compatible), set your API key in your environment:
```bash
# For Gemini (key typically starts with AIza...)
export LLM_API_KEY="AIzaSyYourGeminiApiKeyHere"

# For OpenAI compatible endpoints
export LLM_API_KEY="sk-proj-YourOpenAiKeyHere"
export LLM_BASE_URL="https://api.openai.com/v1/chat/completions" # optional, defaults to OpenAI
```
> **Security Notice**: Never commit `.env` or API keys. If `LLM_API_KEY` is omitted, the service gracefully falls back to rule-based parsing or returns a `503 SERVICE_UNAVAILABLE` error shape if an LLM parser is explicitly demanded.

---

## 3. TaskApi Operations Table & Examples

Every method in `TaskApi` returns an `ApiResponse` holding an HTTP status code and a JSON formatted body.

| Operation | Parameters | Success Status | Error Statuses | Description |
|---|---|---|---|---|
| `createTask` | `TaskRequest request, String idempotencyKey` | `201 Created` | `400`, `422`, `500` | Creates a new task. Replays idempotently if key matches. |
| `getTask` | `int id` | `200 OK` | `404`, `500` | Retrieves a task by unique ID. |
| `getAllTasks` | *None* | `200 OK` | `500` | Retrieves all tasks currently persisted. |
| `updateTask` | `int id, TaskRequest request` | `200 OK` | `400`, `404`, `500` | Updates mutable task fields. |
| `deleteTask` | `int id` | `200 OK` | `404`, `500` | Deletes a task and cascades deletion to all subtasks. |
| `createTaskFromText` | `String text, String idempotencyKey` | `201 Created` | `422`, `502`, `503`, `500` | Parses natural English via LLM into structured task. |
| `createSubtask` | `int parentId, TaskRequest request` | `201 Created` | `400`, `404`, `500` | Creates a subtask linked to parent. Rejects sub-subtasks. |
| `listSubtasks` | `int parentId` | `200 OK` | `404`, `500` | Lists all direct subtasks of a parent task. |
| `breakDownTask` | `int id, String idempotencyKey` | `201 Created` | `400`, `404`, `422`, `502`, `500` | Uses LLM or rule strategy to break down task. |
| `queryTasks` | `TaskQuery query` | `200 OK` | `500` | Filters, searches, sorts, and paginates tasks. |

### Concrete Examples & Printed Responses

#### Example 1: Task Creation (`createTask`)
```java
TaskRequest request = new TaskRequest("Submit quarterly report", "Compile Q4 financial metrics", "false", "2026-12-15", "HIGH");
ApiResponse response = api.createTask(request);
System.out.println(response);
```
**Printed Response:**
```
HTTP 201
{"id":1,"title":"Submit quarterly report","description":"Compile Q4 financial metrics","completed":false,"dueDate":"2026-12-15","priority":"HIGH","recurrence":null,"parentId":null,"createdAt":"2026-11-26T09:54:08.955277Z"}
```

#### Example 2: Validation Error (`createTask` with invalid input)
```java
TaskRequest invalid = new TaskRequest("", "", "maybe", "2026-02-30", "SUPER_HIGH");
ApiResponse response = api.createTask(invalid);
System.out.println(response);
```
**Printed Response:**
```
HTTP 400
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      {"field":"title","issue":"must not be empty"},
      {"field":"description","issue":"must not be empty"},
      {"field":"completed","issue":"must be exactly 'true' or 'false'"},
      {"field":"dueDate","issue":"must be a real YYYY-MM-DD date"},
      {"field":"priority","issue":"must be LOW, MEDIUM, or HIGH"}
    ]
  }
}
```

#### Example 3: AI-Native Task Creation from Text (`createTaskFromText`)
```java
ApiResponse response = api.createTaskFromText("Remind me to pay rent on the 1st of every month");
System.out.println(response);
```
**Printed Response:**
```
HTTP 201
{"id":2,"title":"Pay rent on the 1st of every month","description":"Remind me to pay rent on the 1st of every month","completed":false,"dueDate":null,"priority":"MEDIUM","recurrence":"MONTHLY","parentId":null,"createdAt":"2026-11-26T09:54:09.017201Z"}
```

#### Example 4: Automated Task Breakdown (`breakDownTask`)
```java
ApiResponse response = api.breakDownTask(3);
System.out.println(response);
```
**Printed Response:**
```
HTTP 201
[
  {"id":5,"title":"Book venue","description":"Part of: Organize Tech Conference","completed":false,"dueDate":"2026-11-30","priority":"HIGH","recurrence":null,"parentId":3,"createdAt":"2026-11-26T09:54:09.019415Z"},
  {"id":6,"title":"Invite speakers","description":"Part of: Organize Tech Conference","completed":false,"dueDate":"2026-11-30","priority":"HIGH","recurrence":null,"parentId":3,"createdAt":"2026-11-26T09:54:09.019492Z"},
  {"id":7,"title":"Sell tickets","description":"Part of: Organize Tech Conference","completed":false,"dueDate":"2026-11-30","priority":"HIGH","recurrence":null,"parentId":3,"createdAt":"2026-11-26T09:54:09.019594Z"}
]
```

#### Example 5: Idempotency Conflict (`422 Conflict`)
```java
// Reusing same idempotency key with a changed payload
ApiResponse response = api.createTask(alteredRequest, "client-tx-9901");
System.out.println(response);
```
**Printed Response:**
```
HTTP 422
{
  "error": {
    "code": "CONFLICT",
    "message": "Idempotency key 'client-tx-9901' was already used with a different request payload",
    "details": []
  }
}
```

---

## 4. Design Patterns Implemented

| Pattern | Location / Classes | Purpose & How it is Used |
|---|---|---|
| **Builder** | `model.Task.Builder`<br>`model.TaskQuery.Builder` | Enforces immutability on domain models (`Task`). Requires `title`, `description`, and `completed` while keeping `dueDate`, `priority`, `recurrence`, and `parentId` optional. `TaskQuery.Builder` allows fluid construction of multi-dimensional search/filter queries. |
| **Singleton** | `repository.InMemoryTaskRepository`<br>`service.IdempotencyStore` | Provides single shared, thread-safe repositories. Implemented with lazy double-checked locking, private constructors, and thread-safe data structures (`ConcurrentHashMap`, `Collections.synchronizedMap`). |
| **Factory** | `api.ErrorResponseFactory`<br>`parser.ParserFactory`<br>`service.BreakdownStrategyFactory` | Centralizes object creation. `ErrorResponseFactory.from(...)` guarantees that all exceptions yield the exact required JSON shape. `ParserFactory` and `BreakdownStrategyFactory` instantiate either LLM or rule-based strategies depending on key presence. |
| **Strategy** | `parser.TaskParser` (`LlmTaskParser`, `RuleBasedTaskParser`)<br>`service.BreakdownStrategy` (`LlmBreakdownStrategy`, `RuleBasedBreakdownStrategy`)<br>`service.TaskFilter`<br>`service.SortStrategy` | Makes algorithms interchangeable at runtime. The service consumes interfaces without knowing whether an LLM or regex rule was used. Satisfies the **Open/Closed Principle**: new filters and sorting criteria are added by introducing new classes without editing `TaskService`. |
| **Observer** | `event.TaskEventListener`<br>`event.ConsoleLogListener`<br>`event.AuditTrailListener`<br>`service.TaskService` (Subject) | Decouples notification and side-effect handling from the core service. `TaskService` maintains a thread-safe listener list and announces lifecycle events (`CREATED`, `UPDATED`, `COMPLETED`, `DELETED`, `SUBTASK_CREATED`). Main proves listener addition and dynamic removal. |

---

## 5. LLM Prompt Design

### 5.1 Task Extraction Prompt (`LlmTaskParser`)
```
You are a precise task extraction assistant. Today's date is {today} (YYYY-MM-DD).
Extract structured task details from the text below.
You MUST reply with ONLY a single valid JSON object and no surrounding text or formatting:
{
  "title": "concise task title",
  "description": "descriptive summary of the task",
  "dueDate": "YYYY-MM-DD or null",
  "recurrence": "e.g. 1st of every month, WEEKLY, or null",
  "priority": "LOW, MEDIUM, or HIGH"
}

Input text: "{text}"
```
#### Why it was designed this way:
1. **Reference Date Injection (`{today}`)**: Relative dates like "next Friday", "tomorrow", or "end of month" cannot be accurately grounded without knowing today's wall-clock date.
2. **Strict Output Constraint**: Directs the model to output *only* valid JSON, preventing conversational filler like "Sure, here is your task:".
3. **Explicit Schema Anchoring**: Providing an explicit key-value template prevents hallucinated field names.
4. **"Never Trust the Model" Defense**: The model output is stripped of any accidental markdown fences, parsed via `SimpleJson`, converted into a `TaskRequest`, and validated through `validateRequest`. If the model invents an impossible date like `2026-02-30` or leaves `title` empty, the service rejects it with `502 BAD_GATEWAY` and saves nothing.

### 5.2 Task Breakdown Prompt (`LlmBreakdownStrategy`)
```
You are an expert project planner. Break down the following task into 2 to 4 concrete subtasks.
You MUST output ONLY one subtask per line in the exact format:
title|description
No bullets, no numbers, no headers, no json, no markdown.

Task title: {task.getTitle()}
Task description: {task.getDescription()}
```
#### Why it was designed this way:
1. **Delimited Line Format (`title|description`)**: Line-based plain text parsing is significantly less fragile than requiring the model to generate nested JSON arrays, minimizing syntax corruption.
2. **Explicit Negative Constraints**: "No bullets, no numbers..." prevents the model from injecting numeric lists (`1. `, `2. `) that would otherwise contaminate the subtask titles.
3. **All-or-Nothing Atomicity**: Every generated line is parsed and validated *first*. If any subtask is malformed, none are persisted, returning `502 BAD_GATEWAY`.

---

## 6. AI Testing Critique & Hardening

When initially using an AI assistant to generate a test checklist and test cases, several critical edge cases and security/domain constraints were overlooked:

### Gaps Identified in Naive AI-Generated Tests
1. **Calendar Validation Blindspots**: Naive tests only checked standard dates (`2026-12-01`) and format regex (`\d{4}-\d{2}-\d{2}`), completely missing calendar impossibilities like `2026-02-30`, `2026-04-31`, and leap-year rules (`2023-02-29` rejected vs `2024-02-29` accepted).
2. **Idempotency Key Collisions**: Naive tests tested replaying the *same* payload, but missed the negative security case: using the *same key with different payload data* (which must yield `422 CONFLICT`).
3. **Idempotency Failure Caching**: Naive tests failed to verify that error responses (400) should *not* be cached, allowing a client to correct invalid input and retry with the same key.
4. **Pagination Index Out of Bounds**: The test generator assumed queries only requested valid pages. It missed testing page numbers beyond total pages (e.g. page 99), which initially could trigger sublist `IndexOutOfBoundsException`.
5. **Cascading Delete Verification**: Naive tests deleted a task and checked `findById(parentId) == null`, but forgot to verify that all child subtasks were also purged from the repository.
6. **Subtask Nesting Depth**: Missed testing the domain rule that a subtask cannot itself be broken down or given subtasks (must yield `400 VALIDATION_ERROR`).
7. **External Model Failure Injections**: Naive tests only tested `FakeLlmClient` with valid outputs. They missed simulating network timeouts, non-JSON text, empty titles, and impossible dates produced by the LLM.

### Hardening Measures Implemented
- Created `FaultyLlmClient` with configurable failure modes (`THROW_NETWORK_EXCEPTION`, `RETURN_MALFORMED_JSON`, `RETURN_EMPTY_TITLE`, `RETURN_IMPOSSIBLE_DATE`).
- Hardened `DateValidator` with strict year, month, day-of-month, and leap-year calculation rules.
- Implemented payload fingerprinting in `IdempotencyStore` to detect and reject modified payloads with `422 CONFLICT`.
- Added defensive slicing in `TaskService.queryTasks` returning an empty page rather than throwing on out-of-range pages.
- Expanded `TestRunner` from a basic smoke check into a comprehensive 33-test suite covering all boundary and fault-injection cases.
