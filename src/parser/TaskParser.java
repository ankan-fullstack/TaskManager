package parser;

import model.ParsedTask;

/**
 * Strategy interface for parsing unstructured English text into structured task data.
 */
public interface TaskParser {

    /**
     * Parses free-form text input into a structured ParsedTask.
     *
     * @param text  the user's unstructured request (e.g., "Pay rent next Friday")
     * @param today current reference date in YYYY-MM-DD format for relative date resolution
     * @return parsed task data
     * @throws exception.ApiException if parsing or upstream model call fails
     */
    ParsedTask parse(String text, String today);
}
