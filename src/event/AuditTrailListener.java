package event;

import model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concrete observer maintaining counts of lifecycle events and an audit trail history.
 */
public class AuditTrailListener implements TaskEventListener {

    private final Map<String, AtomicInteger> eventCounts = new ConcurrentHashMap<>();
    private final List<String> history = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onEvent(String eventType, Task task) {
        eventCounts.computeIfAbsent(eventType, k -> new AtomicInteger(0)).incrementAndGet();
        String entry = eventType + " for Task " + (task != null ? task.getId() : "null");
        history.add(entry);
    }

    public int getCount(String eventType) {
        AtomicInteger count = eventCounts.get(eventType);
        return count != null ? count.get() : 0;
    }

    public int getTotalCount() {
        int total = 0;
        for (AtomicInteger count : eventCounts.values()) {
            total += count.get();
        }
        return total;
    }

    public List<String> getHistory() {
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    public void clear() {
        eventCounts.clear();
        history.clear();
    }
}
