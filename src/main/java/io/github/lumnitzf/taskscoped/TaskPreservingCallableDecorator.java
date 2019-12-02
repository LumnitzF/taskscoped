package io.github.lumnitzf.taskscoped;

import javax.enterprise.inject.spi.BeanManager;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Decorator for {@link Callable}, providing the {@link TaskPreserving} implementation.
 * The decorator stores the current {@link TaskId} when being created, and wraps the delegate call in the respective
 * TaskScope.
 *
 * @param <V> the result type of method {@code call}
 * @author Fritz Lumnitz
 */
class TaskPreservingCallableDecorator<V> implements Callable<V> {

    /**
     * The {@link TaskId} when being created.
     */
    private final TaskId taskId;

    /**
     * BeanManager to acquire the {@link TaskScopedContext} when being executed.
     */
    private final BeanManager beanManager;

    /**
     * The delegate {@link #call()} is wrapped for.
     */
    private final Callable<V> delegate;

    TaskPreservingCallableDecorator(BeanManager beanManager, Callable<V> delegate) {
        Objects.requireNonNull(beanManager, "beanManager");
        Objects.requireNonNull(delegate, "delegate");
        this.taskId = TaskIdManager.get().orElseThrow(Exceptions::taskScopeNotActive);
        this.beanManager = beanManager;
        this.delegate = delegate;
    }

    @Override
    public V call() throws Exception {
        final TaskScopedContext context = (TaskScopedContext) beanManager.getContext(TaskScoped.class);
        final TaskId previous = context.enter(taskId);
        try {
            return delegate.call();
        } finally {
            context.exit(previous);
        }
    }
}
