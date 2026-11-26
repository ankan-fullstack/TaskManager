package service;

import exception.ServiceUnavailableException;
import parser.HttpLlmClient;
import parser.LlmClient;

/**
 * Factory for creating BreakdownStrategy instances (Strategy & Factory patterns).
 */
public final class BreakdownStrategyFactory {

    private BreakdownStrategyFactory() {
    }

    public static BreakdownStrategy createLlmStrategy() {
        String apiKey = System.getenv("LLM_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ServiceUnavailableException("LLM_API_KEY is not configured for LLM task breakdown");
        }
        return new LlmBreakdownStrategy(new HttpLlmClient(apiKey));
    }

    public static BreakdownStrategy createLlmStrategy(LlmClient client) {
        if (client == null) {
            throw new IllegalArgumentException("LlmClient cannot be null");
        }
        return new LlmBreakdownStrategy(client);
    }

    public static BreakdownStrategy createRuleBasedStrategy() {
        return new RuleBasedBreakdownStrategy();
    }

    public static BreakdownStrategy createDefaultStrategy() {
        String apiKey = System.getenv("LLM_API_KEY");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return new LlmBreakdownStrategy(new HttpLlmClient(apiKey));
        }
        return new RuleBasedBreakdownStrategy();
    }
}
