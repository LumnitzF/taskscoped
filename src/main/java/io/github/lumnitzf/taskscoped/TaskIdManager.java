package io.github.lumnitzf.taskscoped;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class TaskIdManager {

    private static final ThreadLocal<AtomicReference<TaskId>> CURRENT = ThreadLocal.withInitial(AtomicReference::new);

    // Required for CDI Proxy
    TaskIdManager() {
    }

    static Optional<TaskId> get() {
        final Optional<TaskId> current = Optional.of(CURRENT.get()).map(AtomicReference::get);
        if (!current.isPresent()) {
            CURRENT.remove();
        }
        return current;
    }

    static TaskId getOrCreate() {
        final AtomicReference<TaskId> ref = CURRENT.get();
        TaskId taskId = ref.get();
        if (taskId == null) {
            taskId = TaskId.create();
            ref.set(taskId);
        }
        return taskId;
    }

    static void remove() {
        CURRENT.remove();
    }

    static void set(TaskId taskId) {
        CURRENT.get().set(taskId);
    }

    @Produces
    @TaskScoped
    public TaskId getId() {
        return get().orElseThrow(Exceptions::taskScopeNotActive);
    }
}
