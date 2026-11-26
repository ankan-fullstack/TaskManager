package exception;

/**
 * Thrown when an idempotency conflict occurs (HTTP 422).
 * e.g., reusing an idempotency key with different payload parameters.
 */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(422, "CONFLICT", message);
    }
}
