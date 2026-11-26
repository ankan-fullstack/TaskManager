package service;

import model.Task;
import model.TaskRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Offline rule-based breakdown strategy.
 * Splits task descriptions or titles on commas or 'and' conjunctions.
 */
public class RuleBasedBreakdownStrategy implements BreakdownStrategy {

    @Override
    public List<TaskRequest> breakDown(Task parentTask) {
        if (parentTask == null) {
            throw new IllegalArgumentException("Parent task cannot be null");
        }

        String target = parentTask.getDescription();
        if (target == null || target.trim().isEmpty() || target.equalsIgnoreCase(parentTask.getTitle())) {
            target = parentTask.getTitle();
        }

        List<TaskRequest> requests = new ArrayList<>();
        // Split on commas or " and "
        String[] parts = target.split(",|\\band\\b");

        String priorityStr = parentTask.getPriority() != null ? parentTask.getPriority().name() : "MEDIUM";
        String dueDate = parentTask.getDueDate();

        for (String part : parts) {
            String trimmed = part.trim();
            // Strip leading words like "and", "then", "also"
            trimmed = trimmed.replaceFirst("^(and|then|also)\\s+", "").trim();

            if (!trimmed.isEmpty() && trimmed.length() > 2) {
                String subTitle = Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
                String subDesc = "Part of: " + parentTask.getTitle();
                requests.add(new TaskRequest(subTitle, subDesc, "false", dueDate, priorityStr));
            }
        }

        // If no split was found, provide standard breakdown phases
        if (requests.size() < 2) {
            requests.clear();
            requests.add(new TaskRequest("Plan: " + parentTask.getTitle(), "Initial planning and preparation", "false", dueDate, priorityStr));
            requests.add(new TaskRequest("Execute: " + parentTask.getTitle(), "Direct execution of task requirements", "false", dueDate, priorityStr));
            requests.add(new TaskRequest("Review: " + parentTask.getTitle(), "Final review and verification", "false", dueDate, priorityStr));
        }

        return requests;
    }
}
