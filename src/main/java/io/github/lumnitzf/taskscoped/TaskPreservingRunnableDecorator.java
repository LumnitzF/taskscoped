package io.github.lumnitzf.taskscoped;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import java.util.Objects;

@Decorator
@Priority(Interceptor.Priority.LIBRARY_AFTER)
public abstract class TaskPreservingRunnableDecorator implements Runnable {

    private final TaskId taskId;

    private final BeanManager beanManager;

    private final Runnable delegate;

    @Inject
    TaskPreservingRunnableDecorator(BeanManager beanManager, @Delegate @TaskPreserving Runnable delegate) {
        this.taskId = TaskIdManager.get().orElseThrow(Exceptions::taskScopeNotActive);
        this.beanManager = beanManager;
        this.delegate = delegate;
    }

    public static Runnable decorate(Runnable delegate) {
        Objects.requireNonNull(delegate);
        return new TaskPreservingRunnableDecorator(CDI.current().getBeanManager(), delegate) {
        };
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
