package parser;

import model.ParsedTask;

import java.time.LocalDate;

/**
 * Offline rule-based implementation of TaskParser.
 * Operates purely with string pattern matching and date calculation without external calls.
 */
public class RuleBasedTaskParser implements TaskParser {

    @Override
    public ParsedTask parse(String text, String today) {
        if (text == null || text.trim().isEmpty()) {
            return new ParsedTask("", "", null, null, "MEDIUM");
        }

        String lower = text.toLowerCase().trim();
        String title = text.trim();
        String description = text.trim();
        String dueDate = null;
        String recurrence = null;
        String priority = "MEDIUM";

        // Detect priority
        if (lower.contains("urgent") || lower.contains("asap") || lower.contains("high priority") || lower.contains("critical")) {
            priority = "HIGH";
        } else if (lower.contains("low priority") || lower.contains("whenever") || lower.contains("someday")) {
            priority = "LOW";
        }

        // Detect recurrence
        if (lower.contains("every month") || lower.contains("monthly")) {
            recurrence = "MONTHLY";
        } else if (lower.contains("every week") || lower.contains("weekly")) {
            recurrence = "WEEKLY";
        } else if (lower.contains("every day") || lower.contains("daily")) {
            recurrence = "DAILY";
        } else if (lower.contains("1st of every month")) {
            recurrence = "1st of every month";
        }

        // Relative date parsing for 'tomorrow'
        if (lower.contains("tomorrow")) {
            try {
                LocalDate refDate = (today != null && DateValidator.isValidDate(today))
                        ? LocalDate.parse(today)
                        : LocalDate.now();
                dueDate = refDate.plusDays(1).toString();
            } catch (Exception ignored) {
            }
        }

        // Explicit YYYY-MM-DD date detection
        for (String word : text.split("\\s+")) {
            String cleanWord = word.replaceAll("[,.;:!?()]+$", "");
            if (DateValidator.isValidDate(cleanWord)) {
                dueDate = cleanWord;
                break;
            }
        }

        // Clean up common prefixes for title (e.g. "Remind me to...", "Please...")
        String cleanedTitle = text;
        String[] prefixes = {"remind me to ", "remember to ", "please ", "need to ", "i need to "};
        for (String prefix : prefixes) {
            if (cleanedTitle.toLowerCase().startsWith(prefix)) {
                cleanedTitle = cleanedTitle.substring(prefix.length()).trim();
                break;
            }
        }

        // Capitalize first letter of title
        if (!cleanedTitle.isEmpty()) {
            title = Character.toUpperCase(cleanedTitle.charAt(0)) + (cleanedTitle.length() > 1 ? cleanedTitle.substring(1) : "");
        }

        return new ParsedTask(title, description, dueDate, recurrence, priority);
    }
}
