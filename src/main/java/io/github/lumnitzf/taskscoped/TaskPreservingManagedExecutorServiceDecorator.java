package io.github.lumnitzf.taskscoped;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.inject.spi.BeanManager;
import java.util.concurrent.ExecutorService;

/**
 * Class used as type safe decorator for {@link ManagedExecutorService}.
 * <p>
 * Is not registered as decorator via @{@link javax.decorator.Decorator @Decorator} because the superclass will also be present.
 * Is used for automatic wrapping the result of producer methods for {@link ManagedExecutorService}.
 * </p>
 */
public class TaskPreservingManagedExecutorServiceDecorator extends TaskPreservingExecutorServiceDecorator implements ManagedExecutorService {
    protected TaskPreservingManagedExecutorServiceDecorator(BeanManager beanManager, ExecutorService delegate) {
        super(beanManager, delegate);
    }
}
