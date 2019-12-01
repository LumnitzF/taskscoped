package io.github.lumnitzf.taskscoped;

import javax.enterprise.inject.spi.BeanManager;
import java.util.Objects;

class TaskPreservingRunnableDecorator implements Runnable {

    private final TaskId taskId;

    private final BeanManager beanManager;

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
