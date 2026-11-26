package service;

import model.Task;
import model.TaskQuery;

public class SearchFilter implements TaskFilter {
    @Override
    public boolean matches(Task task, TaskQuery query) {
        String keyword = query.getSearchKeyword();
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        String needle = keyword.trim().toLowerCase();
        boolean matchTitle = task.getTitle() != null && task.getTitle().toLowerCase().contains(needle);
        boolean matchDesc = task.getDescription() != null && task.getDescription().toLowerCase().contains(needle);
        return matchTitle || matchDesc;
    }
}
