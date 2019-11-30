package io.github.lumnitzf.taskscoped;

import org.tomitribe.microscoped.core.ScopeContext;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskScopedContext implements Context {

    private final ScopeContext<TaskId> delegate = new ScopeContext<>(TaskScoped.class);
    private final Map<TaskId, AtomicInteger> activeTasks = new ConcurrentHashMap<>();

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

    public TaskId enter(TaskId taskId) {
        TaskIdManager.set(taskId);
        TaskId previous = delegate.enter(taskId);
        activeTasks.computeIfAbsent(taskId, ignored -> new AtomicInteger(0)).incrementAndGet();
        return previous;
    }

    public TaskId enter() {
        return enter(TaskIdManager.getOrCreate());
    }

    public void exit(TaskId previous) {
        final TaskId taskId = TaskIdManager.get().orElseThrow(Exceptions::taskScopeNotActive);
        delegate.exit(previous);
        if (activeTasks.get(taskId).decrementAndGet() == 0) {
            activeTasks.remove(taskId);
            delegate.destroy(taskId);
        }
        if (previous == null) {
            TaskIdManager.remove();
        } else {
            TaskIdManager.set(previous);
        }
    }
}
