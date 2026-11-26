package model;

import java.util.Objects;

/**
 * Result produced by a TaskParser (either LLM or rule-based) before validation.
 */
public class ParsedTask {
    private final String title;
    private final String description;
    private final String dueDate;
    private final String recurrence;
    private final String priority;

    public ParsedTask(String title, String description, String dueDate, String recurrence, String priority) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.recurrence = recurrence;
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getRecurrence() {
        return recurrence;
    }

    public String getPriority() {
        return priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParsedTask that = (ParsedTask) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(description, that.description) &&
                Objects.equals(dueDate, that.dueDate) &&
                Objects.equals(recurrence, that.recurrence) &&
                Objects.equals(priority, that.priority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, dueDate, recurrence, priority);
    }

    @Override
    public String toString() {
        return "ParsedTask{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", dueDate='" + dueDate + '\'' +
                ", recurrence='" + recurrence + '\'' +
                ", priority='" + priority + '\'' +
                '}';
    }
}
