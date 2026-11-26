package exception;

/**
 * Thrown when a requested resource does not exist (HTTP 404).
 */
public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(404, "NOT_FOUND", message);
    }
}
