package io.github.lumnitzf.taskscoped;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Decorator for {@link Callable}, providing the {@link TaskPreserving} implementation.
 * The decorator stores the current {@link TaskId} when being created, and wraps the delegate call in the respective
 * TaskScope.
 *
 * @param <V> the result type of method {@code call}
 *
 * @author Fritz Lumnitz
 */
class TaskPreservingCallableDecorator<V> implements Callable<V> {

    /**
     * The {@link TaskId} when being created.
     */
    private final TaskId taskId;

    /**
     * The {@link TaskScopedContext} to enter and exit.
     */
    private final TaskScopedContext context;

    /**
     * The delegate {@link #call()} is wrapped for.
     */
    private final Callable<V> delegate;

    /**
     * Flag indicating if the delegate should be {@link TaskScopedContext#unregister(TaskId, Object) unregistered}
     * before its execution.
     */
    private final boolean unregisterOnExecution;

    TaskPreservingCallableDecorator(final TaskScopedContext context, final Callable<V> delegate, final boolean registerOnCreation,
                                    final boolean unregisterOnExecution) {
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
    public V call() throws Exception {
        final TaskId previous = context.enter(taskId);
        if (unregisterOnExecution) {
            context.unregister(taskId, delegate);
        }
        try {
            return delegate.call();
        } finally {
            context.exit(previous);
        }
    }
}
