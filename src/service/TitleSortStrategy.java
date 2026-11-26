package service;

import model.Task;

import java.util.Comparator;

public class TitleSortStrategy implements SortStrategy {
    @Override
    public Comparator<Task> getComparator(String sortOrder) {
        Comparator<Task> comp = Comparator.comparing(
                Task::getTitle,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        );
        return "DESC".equalsIgnoreCase(sortOrder) ? comp.reversed() : comp;
    }
}
