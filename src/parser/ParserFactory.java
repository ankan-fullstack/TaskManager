package parser;

import exception.ServiceUnavailableException;

/**
 * Factory for creating TaskParser instances (Strategy Pattern).
 * Decouples client code from the concrete parser implementation.
 */
public final class ParserFactory {

    private ParserFactory() {
    }

    /**
     * Creates an LLM parser backed by the system environment variable LLM_API_KEY.
     * Throws ServiceUnavailableException (HTTP 503) if the key is missing.
     */
    public static TaskParser createLlmParser() {
        String apiKey = System.getenv("LLM_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ServiceUnavailableException("LLM_API_KEY environment variable is not configured");
        }
        return new LlmTaskParser(new HttpLlmClient(apiKey));
    }

    /**
     * Creates an LLM parser using a provided LlmClient (e.g. for testing with fakes).
     */
    public static TaskParser createLlmParser(LlmClient client) {
        if (client == null) {
            throw new IllegalArgumentException("LlmClient cannot be null");
        }
        return new LlmTaskParser(client);
    }

    /**
     * Creates the offline rule-based parser.
     */
    public static TaskParser createRuleBasedParser() {
        return new RuleBasedTaskParser();
    }

    /**
     * Automatically selects an LLM parser if LLM_API_KEY is present,
     * otherwise falls back gracefully to the rule-based parser.
     */
    public static TaskParser createDefaultParser() {
        String apiKey = System.getenv("LLM_API_KEY");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return new LlmTaskParser(new HttpLlmClient(apiKey));
        }
        return new RuleBasedTaskParser();
    }
}
