package io.github.lumnitzf.taskscoped.beans;

import io.github.lumnitzf.taskscoped.TaskId;
import io.github.lumnitzf.taskscoped.TaskIdManager;
import io.github.lumnitzf.taskscoped.TaskScopeEnabled;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
@TaskScopeEnabled
public class TaskScopeEnabledBean {

    @Inject
    public TaskId taskId;

    @Inject
    public TaskIdManager taskIdManager;

    public <E extends Throwable> void doInTaskScope(ThrowingConsumer<TaskScopeEnabledBean, E> consumer) throws E {
        consumer.accept(this);
    }

    public interface ThrowingConsumer<T, E extends Throwable> {

        void accept(T t) throws E;
    }
}
