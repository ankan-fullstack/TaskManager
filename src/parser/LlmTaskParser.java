package parser;

import exception.BadGatewayException;
import model.ParsedTask;

import java.util.Map;

/**
 * AI-native implementation of TaskParser that delegates to an LLM via LlmClient.
 * Employs strict prompt engineering and robust error handling.
 */
public class LlmTaskParser implements TaskParser {

    private final LlmClient llmClient;

    public LlmTaskParser(LlmClient llmClient) {
        if (llmClient == null) {
            throw new IllegalArgumentException("LlmClient cannot be null");
        }
        this.llmClient = llmClient;
    }

    @Override
    public ParsedTask parse(String text, String today) {
        if (text == null || text.trim().isEmpty()) {
            throw new BadGatewayException("Input text to parse is empty");
        }

        String prompt = buildPrompt(text, today);
        String rawResponse;
        try {
            rawResponse = llmClient.chat(prompt);
        } catch (Exception e) {
            throw new BadGatewayException("LLM request failed or timed out: " + e.getMessage(), e);
        }

        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            throw new BadGatewayException("LLM returned an empty response");
        }

        try {
            Map<String, String> json = SimpleJson.parseObject(rawResponse);
            if (json.isEmpty()) {
                throw new BadGatewayException("LLM response could not be parsed as valid JSON");
            }

            String title = json.get("title");
            String description = json.get("description");
            String dueDate = json.get("dueDate");
            String recurrence = json.get("recurrence");
            String priority = json.get("priority");

            // Normalize null strings
            if ("null".equalsIgnoreCase(dueDate)) dueDate = null;
            if ("null".equalsIgnoreCase(recurrence)) recurrence = null;
            if ("null".equalsIgnoreCase(priority)) priority = null;

            return new ParsedTask(title, description, dueDate, recurrence, priority);
        } catch (BadGatewayException bge) {
            throw bge;
        } catch (Exception e) {
            throw new BadGatewayException("Failed to decode LLM response into task structure: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String text, String today) {
        return "You are a precise task extraction assistant. Today's date is " + today + " (YYYY-MM-DD).\n" +
                "Extract structured task details from the text below.\n" +
                "You MUST reply with ONLY a single valid JSON object and no surrounding text or formatting:\n" +
                "{\n" +
                "  \"title\": \"concise task title\",\n" +
                "  \"description\": \"descriptive summary of the task\",\n" +
                "  \"dueDate\": \"YYYY-MM-DD or null\",\n" +
                "  \"recurrence\": \"e.g. 1st of every month, WEEKLY, or null\",\n" +
                "  \"priority\": \"LOW, MEDIUM, or HIGH\"\n" +
                "}\n\n" +
                "Input text: \"" + SimpleJson.escape(text) + "\"";
    }
}
