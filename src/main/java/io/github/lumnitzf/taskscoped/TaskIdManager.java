package io.github.lumnitzf.taskscoped;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides proxy free access to the current {@link TaskId}
 *
 * @author Fritz Lumnitz
 */
@ApplicationScoped
public class TaskIdManager {

    /**
     * The current set TaskId for this thread.
     * <p>
     * Wrapping in an {@link AtomicReference} provides the ability to acquire and change this value easily without
     * multiple lookups in the ThreadLocal internal map.
     */
    private static final ThreadLocal<AtomicReference<TaskId>> CURRENT = ThreadLocal.withInitial(AtomicReference::new);

    // Required for CDI Proxy
    TaskIdManager() {
    }

    /**
     * Returns the current set TaskId. Does not create an entry in the ThreadLocal store, if no value is set.
     *
     * @return The current set TaskId
     */
    static Optional<TaskId> get() {
        final Optional<TaskId> current = Optional.of(CURRENT.get()).map(AtomicReference::get);
        if (!current.isPresent()) {
            CURRENT.remove();
        }
        return current;
    }

    /**
     * @return The current set TaskId. Creates and stores a new one if none is already set.
     */
    static TaskId getOrCreate() {
        final AtomicReference<TaskId> ref = CURRENT.get();
        TaskId taskId = ref.get();
        if (taskId == null) {
            taskId = TaskId.create();
            ref.set(taskId);
        }
        return taskId;
    }

    /**
     * Remove the current TaskId entry from the ThreadLocal map.
     */
    static void remove() {
        CURRENT.remove();
    }

    /**
     * Set the current TaskId to the provided value.
     *
     * @param taskId The new TaskId.
     */
    static void set(TaskId taskId) {
        CURRENT.get().set(taskId);
    }

    /**
     * @return proxy free instance of the current TaskId
     */
    // Also serves as producer for injection of the current TaskId. However this instance is still wrapped in a proxy
    // because it does not have dependent scope
    @Produces
    @TaskScoped
    public TaskId getId() {
        return get().orElseThrow(Exceptions::taskScopeNotActive);
    }
}
