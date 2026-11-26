package service;

import model.Task;

import java.util.Comparator;

public class IdSortStrategy implements SortStrategy {
    @Override
    public Comparator<Task> getComparator(String sortOrder) {
        Comparator<Task> comp = Comparator.comparingInt(Task::getId);
        return "DESC".equalsIgnoreCase(sortOrder) ? comp.reversed() : comp;
    }
}
