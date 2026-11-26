package parser;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic test fake that returns preset responses without network calls or API keys.
 * Tracks invocation count for verifying idempotency and caching behavior.
 */
public class FakeLlmClient implements LlmClient {
    private String response;
    private final AtomicInteger callCount = new AtomicInteger(0);

    public FakeLlmClient(String response) {
        this.response = response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public int getCallCount() {
        return callCount.get();
    }

    public void resetCallCount() {
        callCount.set(0);
    }

    @Override
    public String chat(String prompt) {
        callCount.incrementAndGet();
        return response;
    }
}
