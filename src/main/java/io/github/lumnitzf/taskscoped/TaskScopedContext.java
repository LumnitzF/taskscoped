package io.github.lumnitzf.taskscoped;

import org.tomitribe.microscoped.core.ScopeContext;

import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link Context} implementation for {@link TaskScoped}.
 *
 * @author Fritz Lumnitz
 */
public class TaskScopedContext implements Context {

    /**
     * Delegate handling the implementation of bean creation etc.
     */
    private final ScopeContext<TaskId> delegate = new ScopeContext<>(TaskScoped.class);

    /**
     * Keeps track of the amount of tasks that are currently in the context. The context is destroyed once all tasks are
     * executed.
     *
     * @see #isCreated(TaskId)
     * @see #isDestroyed(TaskId)
     */
    private final Map<TaskId, AtomicInteger> currentRunningCount = new ConcurrentHashMap<>();

    /**
     * Keeps track of the instances registered for the {@link TaskId}.
     *
     * @see #register(TaskId, Object)
     * @see #unregister(TaskId, Object)
     */
    private final Map<TaskId, Set<Object>> registeredInstances = new ConcurrentHashMap<>();

    private final BeanManager beanManager;

    public TaskScopedContext(BeanManager beanManager) {
        this.beanManager = Objects.requireNonNull(beanManager);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return delegate.getScope();
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        return delegate.get(contextual, creationalContext);
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return delegate.get(contextual);
    }

    @Override
    public boolean isActive() {
        return delegate.isActive();
    }

    /**
     * Registers the {@code instance} to be executed in the TaskScope identified by {@code taskId} some time in the
     * future. As long as instances are registered for a {@link TaskId}, the context is not destroyed.
     *
     * @param taskId   identifying the TaskScope
     * @param instance to be registered
     * @see #unregister(TaskId, Object)
     */
    public void register(TaskId taskId, Object instance) {
        synchronized (taskId.lock) {
            registeredInstances.computeIfAbsent(taskId, ignored -> new HashSet<>()).add(instance);
        }
    }

    /**
     * Removes the {@code instance} from the {@link #register(TaskId, Object) registered} instances.
     *
     * @param taskId   identifying the TaskScope
     * @param instance to be removed
     * @see #register(TaskId, Object)
     */
    public void unregister(TaskId taskId, Object instance) {
        boolean destroyed = false;
        // TODO: refactor this with isDestroyed
        synchronized (taskId.lock) {
            final Set<Object> registeredInstancesForTask = registeredInstances.getOrDefault(taskId,
                    Collections.emptySet());
            registeredInstancesForTask.remove(instance);
            if (registeredInstancesForTask.isEmpty()) {
                AtomicInteger currentRunning = currentRunningCount.get(taskId);
                if (currentRunning != null && currentRunning.get() == 0) {
                    currentRunningCount.remove(taskId);
                    destroyed = true;
                }
            }
        }
        if (destroyed) {
            beanManager.fireEvent(taskId, new DestroyedLiteral(TaskScoped.class));
        }
    }

    /**
     * Enter or create the task scope identified by {@code taskId}.
     *
     * @param taskId identifying the task scope to enter
     * @return id of the previous task scope
     * @see #exit(TaskId)
     */
    public TaskId enter(TaskId taskId) {
        TaskIdManager.set(taskId);
        TaskId previous = delegate.enter(taskId);
        if (isCreated(taskId)) {
            beanManager.fireEvent(taskId, new InitializedLiteral(TaskScoped.class));
        }
        return previous;
    }

    /**
     * Evaluates if the TaskScope for the provided {@code taskId} was just created.
     *
     * @param taskId identifying the TaskScope
     * @return {@code true} if the TaskScope was just created.
     */
    private boolean isCreated(TaskId taskId) {
        // Hack to know that the supplier was called, to fire the initialized event outside of the synchronized block
        final boolean[] created = {false};
        synchronized (taskId.lock) {
            currentRunningCount.computeIfAbsent(taskId, ignored -> {
                created[0] = true;
                return new AtomicInteger(0);
            }).incrementAndGet();
        }
        return created[0];
    }

    /**
     * Enter or create the task scope identified by the {@link TaskIdManager#getOrCreate() current} task id.
     *
     * @return id of the previous task scope
     */
    public TaskId enter() {
        return enter(TaskIdManager.getOrCreate());
    }

    /**
     * Exit the current task scope and destroy it if no one is in it and no one is {@link #register(TaskId, Object)
     * registered} for it. Re-enter the {@code previous} task scope.
     *
     * @param previous identifier of the previous task scope. May be {@code null}
     */
    public void exit(TaskId previous) {
        final TaskId taskId = TaskIdManager.get().orElseThrow(Exceptions::taskScopeNotActive);
        delegate.exit(previous);
        if (isDestroyed(taskId)) {
            beanManager.fireEvent(taskId, new DestroyedLiteral(TaskScoped.class));
        }
        if (previous != null) {
            TaskIdManager.set(previous);
        } else {
            TaskIdManager.remove();
        }
    }

    /**
     * Destroys the TaskScope for {@code taskId} if no one is using it and no one is {@link #register(TaskId, Object)
     * registered} for it.
     *
     * @param taskId identifying the TaskScope
     * @return {@code true} if the TaskScope was destroyed.
     */
    private boolean isDestroyed(TaskId taskId) {
        boolean destroyed = false;
        synchronized (taskId.lock) {
            final boolean noneInContext = currentRunningCount.get(taskId).decrementAndGet() == 0;
            final boolean noneRegistered = registeredInstances.getOrDefault(taskId, Collections.emptySet())
                    .isEmpty();
            if (noneInContext && noneRegistered) {
                currentRunningCount.remove(taskId);
                registeredInstances.remove(taskId);
                delegate.destroy(taskId);
                destroyed = true;
            }
        }
        return destroyed;
    }


    /**
     * Supports inline instantiation of the {@link Initialized} qualifier.
     *
     * @author Fritz Lumnitz
     */
    // Literal implementation pre CDI 2.0
    private static class InitializedLiteral extends AnnotationLiteral<Initialized> implements Initialized {
        private final Class<? extends Annotation> value;

        private InitializedLiteral(Class<? extends Annotation> value) {
            this.value = value;
        }

        @Override
        public Class<? extends Annotation> value() {
            return value;
        }
    }

    /**
     * Supports inline instantiation of the {@link Destroyed} qualifier.
     *
     * @author Fritz Lumnitz
     */
    // Literal implementation pre CDI 2.0
    private static class DestroyedLiteral extends AnnotationLiteral<Destroyed> implements Destroyed {

        private final Class<? extends Annotation> value;

        private DestroyedLiteral(Class<? extends Annotation> value) {
            this.value = value;
        }

        @Override
        public Class<? extends Annotation> value() {
            return value;
        }
    }
}
