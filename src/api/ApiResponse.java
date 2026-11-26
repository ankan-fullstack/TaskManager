package api;

/**
 * Represents a REST-like API response without requiring any web framework.
 * Holds an HTTP status code and a JSON body.
 */
public class ApiResponse {
    private final int statusCode;
    private final String body;

    public ApiResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body != null ? body : "";
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Serializes the entire ApiResponse to a JSON representation.
     */
    public String toJson() {
        return "{\n" +
                "  \"statusCode\": " + statusCode + ",\n" +
                "  \"body\": " + (body.trim().startsWith("{") || body.trim().startsWith("[") ? body : "\"" + escape(body) + "\"") + "\n" +
                "}";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    public static ApiResponse ok(String body) {
        return new ApiResponse(200, body);
    }

    public static ApiResponse created(String body) {
        return new ApiResponse(201, body);
    }

    public static ApiResponse noContent() {
        return new ApiResponse(204, "");
    }

    public static ApiResponse error(int statusCode, String body) {
        return new ApiResponse(statusCode, body);
    }

    @Override
    public String toString() {
        return "HTTP " + statusCode + "\n" + body;
    }
}
