package exception;

/**
 * Thrown when a required service or configuration (e.g., missing LLM API key)
 * is unavailable (HTTP 503).
 */
public class ServiceUnavailableException extends ApiException {
    public ServiceUnavailableException(String message) {
        super(503, "SERVICE_UNAVAILABLE", message);
    }
}
