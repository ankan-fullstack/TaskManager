package exception;

public class FieldIssue {
    private final String field;
    private final String issue;

    public FieldIssue(String field, String issue) {
        this.field = field;
        this.issue = issue;
    }

    public String getField() {
        return field;
    }

    public String getIssue() {
        return issue;
    }

    public String toJson() {
        return "{\"field\":\"" + escape(field) + "\",\"issue\":\"" + escape(issue) + "\"}";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public String toString() {
        return "FieldIssue{" +
                "field='" + field + '\'' +
                ", issue='" + issue + '\'' +
                '}';
    }
}
