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

/**
 * {@link TaskPreserving} decorator for {@link ExecutorService}. <br>
 * Wraps all provided {@link Runnable} and {@link Callable} to be executed in the same TaskScope as the invoker Thread.
 * Subclasses may use the various {@code decorate(...)} methods to achive the same behavior.
 *
 * @author Fritz Lumnitz
 */
@Decorator
@Priority(Interceptor.Priority.LIBRARY_AFTER)
public class TaskPreservingExecutorServiceDecorator implements ExecutorService {

    /**
     * The {@link BeanManager} required by the applied decorators.
     */
    protected final BeanManager beanManager;

    /**
     * The decorated {@link TaskPreserving} delegate.
     */
    protected final ExecutorService delegate;

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

    @Override
    public void execute(Runnable command) {
        delegate.execute(new TaskPreservingRunnableDecorator(beanManager, command));
    }

    /**
     * Decorates the provided {@link Runnable} to be executed in the same TaskScope as this method invocation.
     *
     * @param runnable The Runnable to decorate.
     * @return The decorated Runnable, {@code null} if {@code runnable} was {@code null}
     */
    protected Runnable decorate(Runnable runnable) {
        return runnable == null ? null : new TaskPreservingRunnableDecorator(beanManager, runnable);
    }

    /**
     * Decorates the provided {@link Callable} to be executed in the same TaskScope as this method invocation.
     *
     * @param callable The Callable to decorate.
     * @return The decorated Callable, {@code null} if {@code callable} was {@code null}
     */
    protected <V> Callable<V> decorate(Callable<V> callable) {
        return callable == null ? null : new TaskPreservingCallableDecorator<>(beanManager, callable);
    }

    /**
     * Decorates all the provided {@link Callable} to be executed in the same TaskScope as this method invocation.
     * {@code null} instances in the collection also be present in the resulting collection.
     *
     * @param tasks The collection of callable to decorate.
     * @return Collection of decorated tasks, {@code null} if {@code tasks} was {@code null}.
     */
    protected <T> Collection<? extends Callable<T>> decorate(Collection<? extends Callable<T>> tasks) {
        return tasks == null ? null : tasks.stream().map(this::decorate).collect(Collectors.toList());
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
