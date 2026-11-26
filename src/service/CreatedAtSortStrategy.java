package service;

import model.Task;

import java.util.Comparator;

public class CreatedAtSortStrategy implements SortStrategy {
    @Override
    public Comparator<Task> getComparator(String sortOrder) {
        Comparator<Task> comp = Comparator.comparing(
                Task::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())
        );
        return "DESC".equalsIgnoreCase(sortOrder) ? comp.reversed() : comp;
    }
}
