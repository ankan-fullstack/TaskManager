package model;

public enum Priority {
    LOW,
    MEDIUM,
    HIGH;

    public static Priority fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Priority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Priority must be LOW, MEDIUM, or HIGH");
        }
    }

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        try {
            Priority.valueOf(value.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
