package io.github.lumnitzf.taskscoped;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

@Decorator
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
abstract class TaskPreservingRunnableDecorator implements Runnable {

    private final TaskId taskId;

    @Inject
    private BeanManager beanManager;

    @Inject
    @Delegate
    @TaskPreserving
    private Runnable delegate;

    TaskPreservingRunnableDecorator() {
        this.taskId = TaskIdManager.getOrCreate();
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
