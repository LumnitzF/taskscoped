package io.github.lumnitzf.taskscoped;

import javax.enterprise.inject.spi.BeanManager;
import java.util.Objects;
import java.util.concurrent.Callable;

class TaskPreservingCallableDecorator<T> implements Callable<T> {

    private final TaskId taskId;

    private final BeanManager beanManager;

    private final Callable<T> delegate;

    TaskPreservingCallableDecorator(BeanManager beanManager, Callable<T> delegate) {
        Objects.requireNonNull(beanManager, "beanManager");
        Objects.requireNonNull(delegate, "delegate");
        this.taskId = TaskIdManager.get().orElseThrow(Exceptions::taskScopeNotActive);
        this.beanManager = beanManager;
        this.delegate = delegate;
    }

    @Override
    public T call() throws Exception {
        final TaskScopedContext context = (TaskScopedContext) beanManager.getContext(TaskScoped.class);
        final TaskId previous = context.enter(taskId);
        try {
            return delegate.call();
        } finally {
            context.exit(previous);
        }
    }
}
