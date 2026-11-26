package exception;

/**
 * Thrown when an external dependency (such as an LLM parser) fails,
 * times out, or produces unparseable/invalid output (HTTP 502).
 */
public class BadGatewayException extends ApiException {
    public BadGatewayException(String message) {
        super(502, "BAD_GATEWAY", message);
    }

    public BadGatewayException(String message, Throwable cause) {
        super(502, "BAD_GATEWAY", message, cause);
    }
}
