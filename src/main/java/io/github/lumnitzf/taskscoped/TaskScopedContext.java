package io.github.lumnitzf.taskscoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.function.Consumer;

/**
 * {@link Context} implementation for {@link TaskScoped}.
 *
 * @author Fritz Lumnitz
 */
public class TaskScopedContext implements Context {

    private static final Logger LOG = LoggerFactory.getLogger(TaskScopedContext.class);

    /**
     * Delegate handling the implementation of bean creation etc.
     */
    private final ScopeContext<TaskId> delegate = new ScopeContext<>(TaskScoped.class);

    /**
     * Keeps track of the amount of tasks that are currently in the context. The context is destroyed once all tasks are
     * executed. Accesses to this map and its value must be synchronized using {@link TaskId#lock}.
     *
     * @see #createIfNecessary(TaskId)
     * @see #exit(TaskId)
     */
    private final Map<TaskId, AtomicInteger> currentRunningCount = new ConcurrentHashMap<>();

    /**
     * Keeps track of the instances registered for the {@link TaskId}. Accesses to this map and its value must be
     * synchronized using {@link TaskId#lock}.
     *
     * @see #register(TaskId, Object)
     * @see #unregister(TaskId, Object)
     */
    private final Map<TaskId, Set<Object>> registeredInstances = new ConcurrentHashMap<>();

    /**
     * {@link BeanManager} used to fire events
     */
    private final BeanManager beanManager;

    public TaskScopedContext(final BeanManager beanManager) {
        this.beanManager = Objects.requireNonNull(beanManager);
    }

    /**
     * Activate the TaskScopedContext for this thread.
     */
    // Must be in sync with isActive()
    public static void activate() {
        LOG.debug("Activating TaskScopedContext");
        TaskIdManager.getOrCreate();
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return delegate.getScope();
    }

    @Override
    public <T> T get(final Contextual<T> contextual, final CreationalContext<T> creationalContext) {
        return delegate.get(contextual, creationalContext);
    }

    @Override
    public <T> T get(final Contextual<T> contextual) {
        return delegate.get(contextual);
    }

    @Override
    // Must be in sync with activate()
    public boolean isActive() {
        // Bug in microscoped-core, that scope is always active
        // return delegate.isActive();
        return TaskIdManager.get().isPresent();
    }

    /**
     * Registers the {@code instance} to be executed in the TaskScope identified by {@code taskId} some time in the
     * future. As long as instances are registered for a {@link TaskId}, the context is not destroyed.
     *
     * @param taskId   identifying the TaskScope
     * @param instance to be registered
     *
     * @see #unregister(TaskId, Object)
     */
    public void register(final TaskId taskId, final Object instance) {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(instance, "instance");
        LOG.debug("Registering {} for task {}", instance, taskId);
        synchronized (taskId.lock) {
            registeredInstances.computeIfAbsent(taskId, ignored -> new HashSet<>()).add(instance);
        }
    }

    /**
     * Removes the {@code instance} from the {@link #register(TaskId, Object) registered} instances.
     *
     * @param taskId   identifying the TaskScope
     * @param instance to be removed
     *
     * @see #register(TaskId, Object)
     */
    public void unregister(final TaskId taskId, final Object instance) {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(instance, "instance");
        LOG.debug("Unregistering {} from task {}", instance, taskId);
        destroyIfPossible(taskId, id -> registeredInstances.getOrDefault(id, Collections.emptySet()).remove(instance));
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
     * Enter or create the task scope identified by {@code taskId}.
     *
     * @param taskId identifying the task scope to enter
     *
     * @return id of the previous task scope
     *
     * @see #exit(TaskId)
     */
    public TaskId enter(final TaskId taskId) {
        Objects.requireNonNull(taskId, "taskId");
        TaskIdManager.set(taskId);
        final TaskId previous = delegate.enter(taskId);
        createIfNecessary(taskId);
        LOG.trace("Entered task {}, previous = {}", taskId, previous);
        if (previous != taskId) {
            fireEnter(taskId);
        }
        return previous;
    }

    /**
     * Exit the current task scope and destroy it if no one is in it and no one is {@link #register(TaskId, Object)
     * registered} for it. Re-enter the {@code previous} task scope.
     *
     * @param previous identifier of the previous task scope. May be {@code null}
     */
    public void exit(final TaskId previous) {
        final TaskId taskId = TaskIdManager.get().orElseThrow(Exceptions::taskScopeNotActive);
        // Fire exit event before exiting the context
        if (previous != taskId) {
            fireExit(taskId);
        }
        delegate.exit(previous);
        LOG.trace("Exited task {}, previous = {}", taskId, previous);
        destroyIfPossible(taskId, id -> currentRunningCount.get(id).decrementAndGet());
        if (previous != null) {
            TaskIdManager.set(previous);
        } else {
            TaskIdManager.remove();
        }
    }

    private void createIfNecessary(final TaskId taskId) {
        // Hack to know that the supplier was called, to fire the initialized event outside of the synchronized block
        final boolean[] created = {false};
        synchronized (taskId.lock) {
            currentRunningCount.computeIfAbsent(taskId, ignored -> {
                created[0] = true;
                return new AtomicInteger(0);
            }).incrementAndGet();
        }
        if (created[0]) {
            LOG.debug("Created task {}", taskId);
            fireInitialized(taskId);
        }
    }

    private void destroyIfPossible(final TaskId taskId, final Consumer<TaskId> synchronizedCleanup) {
        boolean destroyed = false;
        synchronized (taskId.lock) {
            synchronizedCleanup.accept(taskId);
            if (canDestroy(taskId)) {
                destroy(taskId);
                destroyed = true;
            }
        }
        if (destroyed) {
            LOG.debug("Destroyed task {}", taskId);
            fireDestroyed(taskId);
        }
    }

    private boolean canDestroy(final TaskId taskId) {
        synchronized (taskId.lock) {
            final AtomicInteger currentRunning = currentRunningCount.get(taskId);
            // If currentRunning is null, we do not have a created scope (only registered instances) so we cannot destroy it
            return currentRunning != null && currentRunning.get() == 0 && registeredInstances.getOrDefault(taskId,
                    Collections.emptySet()).isEmpty();
        }
    }

    private void destroy(final TaskId taskId) {
        synchronized (taskId.lock) {
            currentRunningCount.remove(taskId);
            registeredInstances.remove(taskId);
            delegate.destroy(taskId);
        }
    }

    private void fireDestroyed(final TaskId taskId) {
        beanManager.fireEvent(taskId, new DestroyedLiteral(TaskScoped.class));
    }

    private void fireInitialized(final TaskId taskId) {
        beanManager.fireEvent(taskId, new InitializedLiteral(TaskScoped.class));
    }

    private void fireEnter(final TaskId taskId) {
        beanManager.fireEvent(taskId, AfterTaskEnter.Literal.INSTANCE);
    }

    private void fireExit(final TaskId taskId) {
        beanManager.fireEvent(taskId, BeforeTaskExit.Literal.INSTANCE);
    }

    /**
     * Supports inline instantiation of the {@link Initialized} qualifier.
     *
     * @author Fritz Lumnitz
     */
    // Literal implementation pre CDI 2.0
    static class InitializedLiteral extends AnnotationLiteral<Initialized> implements Initialized {
        private final Class<? extends Annotation> value;

        InitializedLiteral(final Class<? extends Annotation> value) {
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
    static class DestroyedLiteral extends AnnotationLiteral<Destroyed> implements Destroyed {

        private final Class<? extends Annotation> value;

        DestroyedLiteral(final Class<? extends Annotation> value) {
            this.value = value;
        }

        @Override
        public Class<? extends Annotation> value() {
            return value;
        }
    }
}
