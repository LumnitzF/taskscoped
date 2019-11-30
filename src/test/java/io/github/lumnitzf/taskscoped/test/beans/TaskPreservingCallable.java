package io.github.lumnitzf.taskscoped.test.beans;

import io.github.lumnitzf.taskscoped.TaskId;
import io.github.lumnitzf.taskscoped.TaskPreserving;

import javax.enterprise.context.Dependent;
import java.util.concurrent.Callable;

@Dependent
@TaskPreserving
public class TaskPreservingCallable extends BaseTaskPreserving implements Callable<TaskId> {

    @Override
    public TaskId call() {
        return getAndAssertDirectTaskId();
    }
}
