package io.github.lumnitzf.taskscoped;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Decorator
@Priority(Interceptor.Priority.LIBRARY_AFTER)
public class TaskPreservingExecutorServiceDecorator implements ExecutorService {

    private final BeanManager beanManager;

    private final ExecutorService delegate;

    @Inject
    protected TaskPreservingExecutorServiceDecorator(BeanManager beanManager,
                                                     @Delegate @TaskPreserving ExecutorService delegate) {
        this.beanManager = Objects.requireNonNull(beanManager, "beanManager");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(decorate(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(decorate(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(decorate(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(decorate(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
                                         TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(decorate(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(decorate(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
                           TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(decorate(tasks), timeout, unit);
    }

    private Runnable decorate(Runnable runnable) {
        return runnable == null ? null : new TaskPreservingRunnableDecorator(beanManager, runnable);
    }

    private <V> Callable<V> decorate(Callable<V> callable) {
        return callable == null ? null : new TaskPreservingCallableDecorator<>(beanManager, callable);
    }

    private <T> Collection<? extends Callable<T>> decorate(Collection<? extends Callable<T>> tasks) {
        return tasks == null ? null : tasks.stream().map(this::decorate).collect(Collectors.toList());
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(new TaskPreservingRunnableDecorator(beanManager, command));
    }

    // Only delegated methods without changed behavior

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
}
