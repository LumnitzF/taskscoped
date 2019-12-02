package io.github.lumnitzf.taskscoped;

import javax.enterprise.inject.spi.BeanManager;
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
     * BeanManager to acquire the {@link TaskScopedContext} when being executed.
     */
    private final BeanManager beanManager;

    /**
     * The delegate {@link #run()} is wrapped for
     */
    private final Runnable delegate;

    TaskPreservingRunnableDecorator(BeanManager beanManager, Runnable delegate) {
        Objects.requireNonNull(beanManager, "beanManager");
        Objects.requireNonNull(delegate, "delegate");
        this.taskId = TaskIdManager.get().orElseThrow(Exceptions::taskScopeNotActive);
        this.beanManager = beanManager;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        final TaskScopedContext context = (TaskScopedContext) beanManager.getContext(TaskScoped.class);
        final TaskId previous = context.enter(taskId);
        try {
            delegate.run();
        } finally {
            context.exit(previous);
        }
    }
}
