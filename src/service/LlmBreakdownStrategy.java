package service;

import exception.BadGatewayException;
import model.Task;
import model.TaskRequest;
import parser.LlmClient;

import java.util.ArrayList;
import java.util.List;

/**
 * AI-native implementation of BreakdownStrategy.
 * Prompts the LLM for subtasks formatted strictly as one per line: "title|description".
 */
public class LlmBreakdownStrategy implements BreakdownStrategy {

    private final LlmClient llmClient;

    public LlmBreakdownStrategy(LlmClient llmClient) {
        if (llmClient == null) {
            throw new IllegalArgumentException("LlmClient cannot be null");
        }
        this.llmClient = llmClient;
    }

    @Override
    public List<TaskRequest> breakDown(Task parentTask) {
        if (parentTask == null) {
            throw new BadGatewayException("Parent task cannot be null for breakdown");
        }

        String prompt = "You are an expert project planner. Break down the following task into 2 to 4 concrete subtasks.\n" +
                "You MUST output ONLY one subtask per line in the exact format:\n" +
                "title|description\n" +
                "No bullets, no numbers, no headers, no json, no markdown.\n\n" +
                "Task title: " + parentTask.getTitle() + "\n" +
                "Task description: " + parentTask.getDescription() + "\n";

        String rawOutput;
        try {
            rawOutput = llmClient.chat(prompt);
        } catch (Exception e) {
            throw new BadGatewayException("LLM subtask breakdown request failed: " + e.getMessage(), e);
        }

        if (rawOutput == null || rawOutput.trim().isEmpty()) {
            throw new BadGatewayException("LLM returned an empty breakdown response");
        }

        List<TaskRequest> subtaskRequests = new ArrayList<>();
        String[] lines = rawOutput.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int pipeIdx = trimmed.indexOf('|');
            if (pipeIdx == -1) {
                throw new BadGatewayException("Invalid subtask format from LLM: missing '|' delimiter: " + trimmed);
            }

            String title = trimmed.substring(0, pipeIdx).trim();
            String description = trimmed.substring(pipeIdx + 1).trim();

            if (title.isEmpty()) {
                throw new BadGatewayException("LLM generated subtask with empty title");
            }
            if (description.isEmpty()) {
                description = "Subtask of " + parentTask.getTitle();
            }

            String priorityStr = parentTask.getPriority() != null ? parentTask.getPriority().name() : "MEDIUM";
            subtaskRequests.add(new TaskRequest(title, description, "false", parentTask.getDueDate(), priorityStr));
        }

        if (subtaskRequests.isEmpty()) {
            throw new BadGatewayException("LLM failed to produce any valid subtask items");
        }

        return subtaskRequests;
    }
}
