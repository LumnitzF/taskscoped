package io.github.lumnitzf.taskscoped;

import java.util.Objects;
import java.util.UUID;

/**
 * The unique id of the current task. Will automatically be created once the TaskScope is entered.
 * Beans may inject the current TaskId. If the contextual instance without proxy is required, it can be obtained via the
 * {@link TaskIdManager}.
 *
 * @author Fritz Lumnitz
 */
// The CDI Producer is located at TaskIdManager
public class TaskId {

    /**
     * A lock for internal synchronization on the TaskId.
     */
    // User Applications may synchronize on the TaskId directly, which we don't want to interfere with our internal synchronization
    final Object lock = new Object();

    /**
     * The {@link UUID} identifying this TaskId
     */
    private final UUID value;

    // Make private constructor to avoid subclassing without declaring class as final
    // package private instance may be acquired via create()
    private TaskId() {
        value = UUID.randomUUID();
    }

    /**
     * @return A newly created TaskId
     */
    static TaskId create() {
        return new TaskId();
    }

    /**
     * @return The unique {@link UUID} of this TaskId
     */
    // CDI proxies may not correctly print via toString() so provide this as fallback
    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TaskId taskId = (TaskId) o;
        return value.equals(taskId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "TaskId{" +
                "value=" + value +
                '}';
    }
}
