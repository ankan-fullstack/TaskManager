package model;

import java.util.Objects;

/**
 * Immutable domain model for a Task.
 * All fields are private final, no setters are exposed,
 * and instances are constructed exclusively via the Builder.
 */
public final class Task {
    private final int id;
    private final String title;
    private final String description;
    private final boolean completed;
    private final String dueDate;
    private final Priority priority;
    private final String recurrence;
    private final Integer parentId;
    private final String createdAt;

    private Task(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.completed = builder.completed;
        this.dueDate = builder.dueDate;
        this.priority = builder.priority != null ? builder.priority : Priority.MEDIUM;
        this.recurrence = builder.recurrence;
        this.parentId = builder.parentId;
        this.createdAt = builder.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(this.id)
                .title(this.title)
                .description(this.description)
                .completed(this.completed)
                .dueDate(this.dueDate)
                .priority(this.priority)
                .recurrence(this.recurrence)
                .parentId(this.parentId)
                .createdAt(this.createdAt);
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getDueDate() {
        return dueDate;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getRecurrence() {
        return recurrence;
    }

    public Integer getParentId() {
        return parentId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isSubtask() {
        return parentId != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id &&
                completed == task.completed &&
                Objects.equals(title, task.title) &&
                Objects.equals(description, task.description) &&
                Objects.equals(dueDate, task.dueDate) &&
                priority == task.priority &&
                Objects.equals(recurrence, task.recurrence) &&
                Objects.equals(parentId, task.parentId) &&
                Objects.equals(createdAt, task.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, completed, dueDate, priority, recurrence, parentId, createdAt);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", completed=" + completed +
                ", dueDate='" + dueDate + '\'' +
                ", priority=" + priority +
                ", recurrence='" + recurrence + '\'' +
                ", parentId=" + parentId +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }

    /**
     * Serializes this task into a clean JSON representation without external libraries.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(id).append(",");
        sb.append("\"title\":").append(escapeJson(title)).append(",");
        sb.append("\"description\":").append(escapeJson(description)).append(",");
        sb.append("\"completed\":").append(completed).append(",");
        sb.append("\"dueDate\":").append(dueDate == null ? "null" : escapeJson(dueDate)).append(",");
        sb.append("\"priority\":").append(priority == null ? "null" : escapeJson(priority.name())).append(",");
        sb.append("\"recurrence\":").append(recurrence == null ? "null" : escapeJson(recurrence)).append(",");
        sb.append("\"parentId\":").append(parentId == null ? "null" : parentId).append(",");
        sb.append("\"createdAt\":").append(createdAt == null ? "null" : escapeJson(createdAt));
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    public static final class Builder {
        private int id;
        private String title;
        private String description;
        private Boolean completed;
        private String dueDate;
        private Priority priority;
        private String recurrence;
        private Integer parentId;
        private String createdAt;

        public Builder() {
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder completed(boolean completed) {
            this.completed = completed;
            return this;
        }

        public Builder dueDate(String dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder recurrence(String recurrence) {
            this.recurrence = recurrence;
            return this;
        }

        public Builder parentId(Integer parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder createdAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Task build() {
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalStateException("Task title is required and cannot be blank");
            }
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalStateException("Task description is required and cannot be blank");
            }
            if (completed == null) {
                throw new IllegalStateException("Task completed state is required");
            }
            return new Task(this);
        }
    }
}
