package io.github.lumnitzf.taskscoped;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import java.util.Objects;
import java.util.concurrent.Callable;

@Decorator
@Priority(Interceptor.Priority.LIBRARY_AFTER)
public abstract class TaskPreservingCallableDecorator<T> implements Callable<T> {

    private final TaskId taskId;

    private final BeanManager beanManager;

    private final Callable<T> delegate;

    @Inject
    TaskPreservingCallableDecorator(BeanManager beanManager, @Delegate @TaskPreserving Callable<T> delegate) {
        this.taskId = TaskIdManager.get().orElseThrow(Exceptions::taskScopeNotActive);
        this.beanManager = beanManager;
        this.delegate = delegate;
    }

    public static <T> Callable<T> decorate(Callable<T> delegate) {
        Objects.requireNonNull(delegate);
        return new TaskPreservingCallableDecorator<T>(CDI.current().getBeanManager(), delegate) {
        };
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
