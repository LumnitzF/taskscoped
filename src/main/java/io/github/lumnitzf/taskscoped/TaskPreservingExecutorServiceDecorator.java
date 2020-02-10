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
 * Subclasses may use the various {@code decorate(...)} methods to achieve the same behavior.
 *
 * @author Fritz Lumnitz
 */
@Decorator
@Priority(Interceptor.Priority.LIBRARY_AFTER)
public class TaskPreservingExecutorServiceDecorator implements ExecutorService {

    /**
     * The {@link BeanManager} to get the {@link TaskScopedContext}.
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
        delegate.execute(decorate(command));
    }

    /**
     * Decorates the provided {@link Runnable} to be executed in the same TaskScope as this method invocation.
     * The {@code runnable} is expected to run exactly once in the TaskScope. If it does not run at all, the TaskScope
     * will be held active indefinitely. If it does run more than once, the TaskScope may be destroyed and re-created
     * between the invocations.
     *
     * @param runnable Runnable to decorate.
     * @return decorated Runnable, {@code null} if {@code runnable} was {@code null}
     * @see #decorate(Runnable, boolean, boolean)
     */
    protected Runnable decorate(Runnable runnable) {
        return decorate(runnable, true, true);
    }

    /**
     * Decorates the provided {@link Runnable} to be executed in the same TaskScope as this method invocation.
     *
     * @param runnable              Runnable to decorate.
     * @param registerOnCreation    indicates if the callable should be {@link TaskScopedContext#register(TaskId,
     *                              Object) registered} for execution
     * @param unregisterOnExecution indicates if the callable should be {@link TaskScopedContext#unregister(TaskId,
     *                              Object) unregistered} before its execution.
     * @return decorated Runnable, {@code null} if {@code runnable} was {@code null}
     */
    protected Runnable decorate(Runnable runnable, boolean registerOnCreation, boolean unregisterOnExecution) {
        return runnable == null ? null : new TaskPreservingRunnableDecorator(getContext(), runnable, registerOnCreation, unregisterOnExecution);
    }

    /**
     * Decorates the provided {@link Callable} to be executed in the same TaskScope as this method invocation. The
     * {@code callable} is expected to run exactly once in the TaskScope. If it does not run at all, the TaskScope will
     * be held active indefinitely. If it does run more than once, the TaskScope may be destroyed and re-created between
     * the invocations.
     *
     * @param callable Callable to decorate
     * @param <T>      type of the values returned from the tasks
     * @return decorated Callable, {@code null} if {@code callable} was {@code null}
     * @see #decorate(Callable, boolean, boolean)
     */
    protected <T> Callable<T> decorate(Callable<T> callable) {
        return decorate(callable, true, true);
    }

    /**
     * Decorates the provided {@link Callable} to be executed in the same TaskScope as this method invocation.
     *
     * @param callable              Callable to decorate
     * @param registerOnCreation    indicates if the callable should be {@link TaskScopedContext#register(TaskId,
     *                              Object) registered} for execution
     * @param unregisterOnExecution indicates if the callable should be {@link TaskScopedContext#unregister(TaskId,
     *                              Object) unregistered} before its execution.
     * @param <T>                   the type of the value returned from the callable
     * @return decorated Callable, {@code null} if {@code callable} was {@code null}
     */
    protected <T> Callable<T> decorate(Callable<T> callable, boolean registerOnCreation,
                                       boolean unregisterOnExecution) {
        return callable == null ? null : new TaskPreservingCallableDecorator<>(getContext(), callable,
                registerOnCreation,
                unregisterOnExecution);
    }

    /**
     * Decorates all the provided {@link Callable} to be executed in the same TaskScope as this method invocation.
     * {@code null} instances in the collection also be present in the resulting collection. All
     * tasks are expected to run exactly once in the TaskScope. If it does not run at all, the TaskScope
     * will be held active indefinitely. If it does run more than once, the TaskScope may be destroyed and re-created
     * between the invocations.
     *
     * @param tasks collection of callable to decorate.
     * @param <T>   type of values returned from the callable
     * @return collection of decorated tasks, {@code null} if {@code tasks} was {@code null}.
     * @see #decorate(Collection, boolean, boolean)
     */
    protected <T> Collection<? extends Callable<T>> decorate(Collection<? extends Callable<T>> tasks) {
        return decorate(tasks, true, true);
    }

    /**
     * Decorates all the provided {@link Callable} to be executed in the same TaskScope as this method invocation.
     * {@code null} instances in the collection also be present in the resulting collection.
     *
     * @param tasks                 collection of callable to decorate.
     * @param registerOnCreation    indicates if the tasks should be {@link TaskScopedContext#register(TaskId,
     *                              Object) registered} for execution
     * @param unregisterOnExecution indicates if the tasks should be {@link TaskScopedContext#unregister(TaskId,
     *                              Object) unregistered} before its execution.
     * @param <T>                   the type of values returned from the tasks
     * @return collection of decorated tasks, {@code null} if {@code tasks} was {@code null}.
     */
    protected <T> Collection<? extends Callable<T>> decorate(Collection<? extends Callable<T>> tasks,
                                                             boolean registerOnCreation,
                                                             boolean unregisterOnExecution) {
        return tasks == null ? null : tasks.stream()
                .map(task -> decorate(task, registerOnCreation, unregisterOnExecution)).collect(Collectors.toList());
    }

    private TaskScopedContext getContext() {
        return (TaskScopedContext) beanManager.getContext(TaskScoped.class);
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
