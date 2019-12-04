package io.github.lumnitzf.taskscoped;

import java.util.Objects;

/**
 * Decorator for {@link Runnable}, providing the {@link TaskPreserving} implementation.
 * The decorator stores the current {@link TaskId} when being created, and wraps the delegate call in the respective
 * TaskScope.
 *
 * @author Fritz Lumnitz
 */
class TaskPreservingRunnableDecorator implements Runnable {

    /**
     * The {@link TaskId} when being created.
     */
    private final TaskId taskId;

    /**
     * The {@link TaskScopedContext} to enter and exit.
     */
    private final TaskScopedContext context;

    /**
     * The delegate {@link #run()} is wrapped for
     */
    private final Runnable delegate;

    /**
     * Flag indicating if the delegate should be {@link TaskScopedContext#unregister(TaskId, Object) unregistered}
     * before its execution.
     */
    private final boolean unregisterOnExecution;

    TaskPreservingRunnableDecorator(TaskScopedContext context, Runnable delegate, boolean registerOnCreation,
                                    boolean unregisterOnExecution) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(delegate, "delegate");
        this.taskId = TaskIdManager.get().orElseThrow(Exceptions::taskScopeNotActive);
        this.context = context;
        this.delegate = delegate;
        this.unregisterOnExecution = unregisterOnExecution;
        if (registerOnCreation) {
            context.register(taskId, delegate);
        }
    }

    @Override
    public void run() {
        final TaskId previous = context.enter(taskId);
        if (unregisterOnExecution) {
            context.unregister(taskId, delegate);
        }
        try {
            delegate.run();
        } finally {
            context.exit(previous);
        }
    }
}
