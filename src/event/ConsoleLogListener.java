package event;

import model.Task;

/**
 * Concrete observer logging events directly to the console.
 */
public class ConsoleLogListener implements TaskEventListener {

    @Override
    public void onEvent(String eventType, Task task) {
        String info = task != null
                ? "ID=" + task.getId() + " [\"" + task.getTitle() + "\"]"
                : "N/A";
        System.out.println("[ConsoleLogListener] " + eventType + " -> " + info);
    }
}
