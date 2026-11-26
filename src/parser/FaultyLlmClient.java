package parser;

import java.io.IOException;

/**
 * Test fake simulating external failure modes:
 * network timeouts, network errors, malformed responses, and invalid domain data.
 */
public class FaultyLlmClient implements LlmClient {

    public enum FaultMode {
        THROW_NETWORK_EXCEPTION,
        RETURN_MALFORMED_JSON,
        RETURN_EMPTY_TITLE,
        RETURN_IMPOSSIBLE_DATE,
        RETURN_INVALID_PRIORITY,
        RETURN_EMPTY_STRING
    }

    private FaultMode faultMode;

    public FaultyLlmClient(FaultMode faultMode) {
        this.faultMode = faultMode;
    }

    public void setFaultMode(FaultMode faultMode) {
        this.faultMode = faultMode;
    }

    @Override
    public String chat(String prompt) throws Exception {
        switch (faultMode) {
            case THROW_NETWORK_EXCEPTION:
                throw new IOException("Remote LLM service connection timed out after 5000ms");
            case RETURN_MALFORMED_JSON:
                return "Sure! Here is your task: {title: Pay rent, dueDate: tomorrow (not valid JSON)}";
            case RETURN_EMPTY_TITLE:
                return "{\"title\": \"\", \"description\": \"Valid description\", \"dueDate\": \"2026-12-01\", \"priority\": \"HIGH\"}";
            case RETURN_IMPOSSIBLE_DATE:
                return "{\"title\": \"Pay leap day rent\", \"description\": \"Rent check\", \"dueDate\": \"2026-02-30\", \"priority\": \"HIGH\"}";
            case RETURN_INVALID_PRIORITY:
                return "{\"title\": \"Pay rent\", \"description\": \"Rent check\", \"dueDate\": \"2026-12-01\", \"priority\": \"URGENT_NOW\"}";
            case RETURN_EMPTY_STRING:
                return "";
            default:
                throw new IllegalStateException("Unknown fault mode: " + faultMode);
        }
    }
}
