package service;

import model.Task;

import java.util.Comparator;

/**
 * Strategy interface for sorting tasks.
 * Follows Strategy Pattern and Open/Closed Principle.
 */
public interface SortStrategy {

    /**
     * Supplies a comparator configured for the requested sort order ("ASC" or "DESC").
     */
    Comparator<Task> getComparator(String sortOrder);
}
