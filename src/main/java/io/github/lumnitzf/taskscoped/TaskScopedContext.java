package io.github.lumnitzf.taskscoped;

import org.tomitribe.microscoped.core.ScopeContext;

import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link Context} implementation for {@link TaskScoped}.
 *
 * @author Fritz Lumnitz
 */
// TODO: verify that the Initialized and Destroyed events are fired
public class TaskScopedContext implements Context {

    /**
     * Delegate handling the implementation of bean creation etc.
     */
    private final ScopeContext<TaskId> delegate = new ScopeContext<>(TaskScoped.class);

    /**
     * Keeps track of the amount of tasks that are currently in the context. The context is destroyed once all tasks are
     * executed.
     */
    private final Map<TaskId, AtomicInteger> currentRunningCount = new ConcurrentHashMap<>();

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
     * Enter or create the task scope identified by {@code taskId}.
     *
     * @param taskId identifying the task scope to enter
     * @return The id of the previous task scope
     */
    public TaskId enter(TaskId taskId) {
        TaskIdManager.set(taskId);
        TaskId previous = delegate.enter(taskId);

        // Hack to know that the supplier was called, to fire the initialized event outside of the synchronized block
        final boolean[] created = {false};
        currentRunningCount.computeIfAbsent(taskId, ignored -> {
            created[0] = true;
            return new AtomicInteger(0);
        }).incrementAndGet();
        if (created[0]) {
            CDI.current().getBeanManager().fireEvent(taskId, new InitializedLiteral(TaskScoped.class));
        }
        return previous;
    }

    /**
     * Enter or create the task scope identified by the {@link TaskIdManager#getOrCreate() current} task id.
     *
     * @return The id of the previous task scope
     */
    public TaskId enter() {
        return enter(TaskIdManager.getOrCreate());
    }

    /**
     * Exit the current task scope and re-enter the {@code previous} task scope.
     *
     * @param previous The identifier of the previous task scope. May be {@code null}
     */
    public void exit(TaskId previous) {
        final TaskId taskId = TaskIdManager.get().orElseThrow(Exceptions::taskScopeNotActive);
        delegate.exit(previous);
        if (currentRunningCount.get(taskId).decrementAndGet() == 0) {
            // TODO: currently the TaskScope may be destroyed, if a runnable etc. is scheduled but not yet executed and
            //  the scheduling task scope is left.
            //  --> Implement some sort of pre-registering on creation of delegates and do not destroy until all
            //      pre-registered tasks are executed
            currentRunningCount.remove(taskId);
            delegate.destroy(taskId);
            CDI.current().getBeanManager().fireEvent(taskId, new DestroyedLiteral(TaskScoped.class));
        }
        if (previous != null) {
            TaskIdManager.set(previous);
        } else {
            TaskIdManager.remove();
        }
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
