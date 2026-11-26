# Task Manager API Contract

This document defines the formal API contract for the plain Java Task Manager service. It specifies the input shapes, response structures, status codes, and error semantics for all operations.

---

## 1. Error Response Specification

All errors returned by the service adhere to a uniform, predictable JSON shape across all endpoints.

### Schema
```json
{
  "error": {
    "code": "STRING",
    "message": "STRING",
    "details": [
      {
        "field": "STRING",
        "issue": "STRING"
      }
    ]
  }
}
```

### Status & Error Code Reference Table

| HTTP Status | Error Code | Description | Example Trigger |
|---|---|---|---|
| **400 Bad Request** | `VALIDATION_ERROR` | Request validation failed (blank fields, invalid format, illegal subtask depth). | `title: ""` or adding subtask to a subtask |
| **404 Not Found** | `NOT_FOUND` | The specified resource ID was not found in the repository. | `getTask(999)` |
| **422 Unprocessable Entity** | `CONFLICT` | Idempotency key conflict: the same key was provided with different request data. | Replaying `key-1` with modified title |
| **500 Internal Server Error** | `INTERNAL_SERVER_ERROR` | Unexpected unhandled server exception. | Uncaught runtime failure |
| **502 Bad Gateway** | `BAD_GATEWAY` | External upstream service (LLM) failed, timed out, or produced invalid output. | LLM returns non-JSON or impossible date |
| **503 Service Unavailable** | `SERVICE_UNAVAILABLE` | Required external configuration is absent (e.g., missing API key). | `ParserFactory.createLlmParser()` without `LLM_API_KEY` |

---

## 2. Operations Specification

### 2.1 Create Task
- **Method Signature**: `ApiResponse createTask(TaskRequest request, String idempotencyKey)`
- **Success Status**: `201 Created`
- **Error Statuses**: `400 Bad Request` (validation error), `422 Unprocessable Entity` (idempotency conflict)

#### Request Payload (`TaskRequest`)
```json
{
  "title": "Submit tax audit",
  "description": "Prepare 2026 financial paperwork",
  "completed": "false",
  "dueDate": "2026-12-31",
  "priority": "HIGH",
  "recurrence": null
}
```

#### Success Response (`201 Created`)
```json
{
  "id": 1,
  "title": "Submit tax audit",
  "description": "Prepare 2026 financial paperwork",
  "completed": false,
  "dueDate": "2026-12-31",
  "priority": "HIGH",
  "recurrence": null,
  "parentId": null,
  "createdAt": "2026-11-26T09:54:08.955Z"
}
```

---

### 2.2 Get Task by ID
- **Method Signature**: `ApiResponse getTask(int id)`
- **Success Status**: `200 OK`
- **Error Statuses**: `404 Not Found`

#### Success Response (`200 OK`)
```json
{
  "id": 1,
  "title": "Submit tax audit",
  "description": "Prepare 2026 financial paperwork",
  "completed": false,
  "dueDate": "2026-12-31",
  "priority": "HIGH",
  "recurrence": null,
  "parentId": null,
  "createdAt": "2026-11-26T09:54:08.955Z"
}
```

#### Error Response (`404 Not Found`)
```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "Task with ID 999 not found",
    "details": []
  }
}
```

---

### 2.3 Get All Tasks
- **Method Signature**: `ApiResponse getAllTasks()`
- **Success Status**: `200 OK`

#### Success Response (`200 OK`)
```json
[
  {
    "id": 1,
    "title": "Submit tax audit",
    "description": "Prepare 2026 financial paperwork",
    "completed": false,
    "dueDate": "2026-12-31",
    "priority": "HIGH",
    "recurrence": null,
    "parentId": null,
    "createdAt": "2026-11-26T09:54:08.955Z"
  }
]
```

---

### 2.4 Update Task
- **Method Signature**: `ApiResponse updateTask(int id, TaskRequest request)`
- **Success Status**: `200 OK`
- **Error Statuses**: `400 Bad Request`, `404 Not Found`

#### Success Response (`200 OK`)
```json
{
  "id": 1,
  "title": "Submit tax audit",
  "description": "Completed and filed with auditor",
  "completed": true,
  "dueDate": "2026-12-31",
  "priority": "HIGH",
  "recurrence": null,
  "parentId": null,
  "createdAt": "2026-11-26T09:54:08.955Z"
}
```

---

### 2.5 Delete Task (Cascading)
- **Method Signature**: `ApiResponse deleteTask(int id)`
- **Success Status**: `200 OK`
- **Error Statuses**: `404 Not Found`
- **Behavior**: Deleting a parent automatically cascades and deletes all associated subtasks.

#### Success Response (`200 OK`)
```json
{
  "message": "Task with ID 1 and any subtasks deleted successfully"
}
```

---

### 2.6 Create Task from Natural Language (AI-Native)
- **Method Signature**: `ApiResponse createTaskFromText(String text, String idempotencyKey)`
- **Success Status**: `201 Created`
- **Error Statuses**: `422 Unprocessable Entity` (idempotency conflict), `502 Bad Gateway` (LLM failure / invalid domain output), `503 Service Unavailable` (missing API key)

#### Input Example
```text
"Remind me to pay rent on the 1st of every month"
```

#### Success Response (`201 Created`)
```json
{
  "id": 2,
  "title": "Pay rent on the 1st of every month",
  "description": "Remind me to pay rent on the 1st of every month",
  "completed": false,
  "dueDate": null,
  "priority": "MEDIUM",
  "recurrence": "MONTHLY",
  "parentId": null,
  "createdAt": "2026-11-26T09:54:09.017Z"
}
```

---

### 2.7 Create Subtask
- **Method Signature**: `ApiResponse createSubtask(int parentId, TaskRequest request)`
- **Success Status**: `201 Created`
- **Error Statuses**: `400 Bad Request` (parent is already a subtask, or invalid input), `404 Not Found` (parent does not exist)

#### Success Response (`201 Created`)
```json
{
  "id": 4,
  "title": "Book venue",
  "description": "Finalize convention hall contract",
  "completed": false,
  "dueDate": "2026-11-20",
  "priority": "HIGH",
  "recurrence": null,
  "parentId": 3,
  "createdAt": "2026-11-26T09:54:09.018Z"
}
```

---

### 2.8 List Subtasks
- **Method Signature**: `ApiResponse listSubtasks(int parentId)`
- **Success Status**: `200 OK`
- **Error Statuses**: `404 Not Found` (parent does not exist)

#### Success Response (`200 OK`)
```json
[
  {
    "id": 4,
    "title": "Book venue",
    "description": "Finalize convention hall contract",
    "completed": false,
    "dueDate": "2026-11-20",
    "priority": "HIGH",
    "recurrence": null,
    "parentId": 3,
    "createdAt": "2026-11-26T09:54:09.018Z"
  }
]
```

---

### 2.9 Break Down Task
- **Method Signature**: `ApiResponse breakDownTask(int id, String idempotencyKey)`
- **Success Status**: `201 Created`
- **Error Statuses**: `400 Bad Request` (task is itself a subtask), `404 Not Found`, `422 Unprocessable Entity`, `502 Bad Gateway` (LLM failure or invalid breakdown items)

#### Success Response (`201 Created`)
```json
[
  {
    "id": 5,
    "title": "Book venue",
    "description": "Part of: Organize Tech Conference",
    "completed": false,
    "dueDate": "2026-11-30",
    "priority": "HIGH",
    "recurrence": null,
    "parentId": 3,
    "createdAt": "2026-11-26T09:54:09.019Z"
  },
  {
    "id": 6,
    "title": "Invite speakers",
    "description": "Part of: Organize Tech Conference",
    "completed": false,
    "dueDate": "2026-11-30",
    "priority": "HIGH",
    "recurrence": null,
    "parentId": 3,
    "createdAt": "2026-11-26T09:54:09.019Z"
  }
]
```

---

### 2.10 Query Tasks (Filtering, Search, Sorting, and Pagination)
- **Method Signature**: `ApiResponse queryTasks(TaskQuery query)`
- **Success Status**: `200 OK`

#### Query Parameters (`TaskQuery`)
- `completed`: Boolean (`true` / `false` / `null`)
- `priority`: Priority (`LOW`, `MEDIUM`, `HIGH`, `null`)
- `dueDate`: String (`YYYY-MM-DD` / `null`)
- `searchKeyword`: String (matches title or description case-insensitively)
- `parentId`: Integer (filters by parent ID or null)
- `sortBy`: String (`"dueDate"`, `"priority"`, `"title"`, `"createdAt"`, `"id"`)
- `sortOrder`: String (`"ASC"`, `"DESC"`)
- `page`: Integer (1-indexed, default 1)
- `pageSize`: Integer (default 10)

#### Success Response (`200 OK`)
```json
{
  "page": 1,
  "pageSize": 2,
  "totalTasks": 3,
  "totalPages": 2,
  "tasks": [
    {
      "id": 10,
      "title": "Beta Fix security issue",
      "description": "Review authentication vulnerabilities",
      "completed": false,
      "dueDate": "2026-12-01",
      "priority": "HIGH",
      "recurrence": null,
      "parentId": null,
      "createdAt": "2026-11-26T09:54:09.034Z"
    },
    {
      "id": 9,
      "title": "Alpha Review documentation",
      "description": "Review chapter 1",
      "completed": false,
      "dueDate": "2026-12-05",
      "priority": "LOW",
      "recurrence": null,
      "parentId": null,
      "createdAt": "2026-11-26T09:54:09.034Z"
    }
  ]
}
```
