package io.github.lumnitzf.taskscoped;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Decorator
@Priority(Interceptor.Priority.LIBRARY_AFTER)
public abstract class TaskPreservingExecutorServiceDecorator implements ExecutorService {

    private final ExecutorService delegate;

    @Inject
    TaskPreservingExecutorServiceDecorator(@Delegate @TaskPreserving ExecutorService delegate) {
        this.delegate = delegate;
    }

    public static ExecutorService decorate(ExecutorService delegate) {
        Objects.requireNonNull(delegate);
        return new TaskPreservingExecutorServiceDecorator(delegate) {
            @Override
            public void shutdown() {
                delegate.shutdown();
            }

            @Override
            public List<Runnable> shutdownNow() {
                return delegate.shutdownNow();
            }

            @Override
            public boolean isShutdown() {
                return delegate.isShutdown();
            }

            @Override
            public boolean isTerminated() {
                return delegate.isTerminated();
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return delegate.awaitTermination(timeout, unit);
            }
        };
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(TaskPreservingCallableDecorator.decorate(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(TaskPreservingRunnableDecorator.decorate(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(TaskPreservingRunnableDecorator.decorate(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        Collection<? extends Callable<T>> decoratedTasks = decorate(tasks);
        return delegate.invokeAll(decoratedTasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        Collection<? extends Callable<T>> decoratedTasks = decorate(tasks);
        return delegate.invokeAll(decoratedTasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        Collection<? extends Callable<T>> decoratedTasks = decorate(tasks);
        return delegate.invokeAny(decoratedTasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Collection<? extends Callable<T>> decoratedTasks = decorate(tasks);
        return delegate.invokeAny(decoratedTasks, timeout, unit);
    }

    private <T> Collection<? extends Callable<T>> decorate(Collection<? extends Callable<T>> tasks) {
        return tasks.stream().map(TaskPreservingCallableDecorator::decorate).collect(Collectors.toList());
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(TaskPreservingRunnableDecorator.decorate(command));
    }
}
