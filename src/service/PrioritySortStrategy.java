package service;

import model.Priority;
import model.Task;

import java.util.Comparator;

public class PrioritySortStrategy implements SortStrategy {
    @Override
    public Comparator<Task> getComparator(String sortOrder) {
        // High (3) > Medium (2) > Low (1)
        Comparator<Task> comp = (t1, t2) -> {
            int p1 = score(t1.getPriority());
            int p2 = score(t2.getPriority());
            return Integer.compare(p1, p2);
        };
        return "DESC".equalsIgnoreCase(sortOrder) ? comp.reversed() : comp;
    }

    private int score(Priority p) {
        if (p == null) return 0;
        switch (p) {
            case HIGH: return 3;
            case MEDIUM: return 2;
            case LOW: return 1;
            default: return 0;
        }
    }
}
