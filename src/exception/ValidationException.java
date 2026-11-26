package exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when incoming request validation fails (HTTP 400).
 * Collects multiple field issues so all problems can be reported at once.
 */
public class ValidationException extends ApiException {
    private final List<FieldIssue> details;

    public ValidationException(String message, List<FieldIssue> details) {
        super(400, "VALIDATION_ERROR", message);
        this.details = details != null ? new ArrayList<>(details) : Collections.emptyList();
    }

    public ValidationException(List<FieldIssue> details) {
        this("Validation failed", details);
    }

    public ValidationException(String field, String issue) {
        this("Validation failed", List.of(new FieldIssue(field, issue)));
    }

    public List<FieldIssue> getDetails() {
        return Collections.unmodifiableList(details);
    }
}
