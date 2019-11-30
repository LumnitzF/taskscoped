package io.github.lumnitzf.taskscoped;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import java.util.concurrent.Callable;

@Decorator
@Priority(Interceptor.Priority.LIBRARY_AFTER)
abstract class TaskPreservingCallableDecorator<T> implements Callable<T> {

    private final TaskId taskId;

    @Inject
    private BeanManager beanManager;

    @Inject
    @Delegate
    @TaskPreserving
    private Callable<T> delegate;

    TaskPreservingCallableDecorator() {
        this.taskId = TaskIdManager.getOrCreate();
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
