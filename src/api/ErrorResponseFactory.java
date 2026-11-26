package api;

import exception.ApiException;
import exception.FieldIssue;
import exception.ValidationException;

import java.util.List;

/**
 * Factory for creating uniform API error responses according to the specification.
 * Ensures consistent error shapes across all endpoints.
 */
public final class ErrorResponseFactory {

    private ErrorResponseFactory() {
    }

    public static ApiResponse from(ApiException e) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"error\": {\n");
        sb.append("    \"code\": \"").append(escape(e.getErrorCode())).append("\",\n");
        sb.append("    \"message\": \"").append(escape(e.getMessage())).append("\"");

        if (e instanceof ValidationException) {
            ValidationException ve = (ValidationException) e;
            List<FieldIssue> details = ve.getDetails();
            sb.append(",\n    \"details\": [");
            if (!details.isEmpty()) {
                sb.append("\n");
                for (int i = 0; i < details.size(); i++) {
                    FieldIssue issue = details.get(i);
                    sb.append("      ").append(issue.toJson());
                    if (i < details.size() - 1) {
                        sb.append(",");
                    }
                    sb.append("\n");
                }
                sb.append("    ]");
            } else {
                sb.append("]");
            }
        } else {
            sb.append(",\n    \"details\": []");
        }

        sb.append("\n  }\n");
        sb.append("}");

        return new ApiResponse(e.getStatusCode(), sb.toString());
    }

    public static ApiResponse from(Exception e) {
        if (e instanceof ApiException) {
            return from((ApiException) e);
        }
        String message = e.getMessage() != null ? e.getMessage() : "Internal server error";
        String json = "{\n" +
                "  \"error\": {\n" +
                "    \"code\": \"INTERNAL_SERVER_ERROR\",\n" +
                "    \"message\": \"" + escape(message) + "\",\n" +
                "    \"details\": []\n" +
                "  }\n" +
                "}";
        return new ApiResponse(500, json);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
