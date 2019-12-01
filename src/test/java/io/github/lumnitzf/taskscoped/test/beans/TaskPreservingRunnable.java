package io.github.lumnitzf.taskscoped.test.beans;

import io.github.lumnitzf.taskscoped.TaskPreserving;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
//@TaskPreserving
public class TaskPreservingRunnable extends BaseTaskPreserving implements Runnable {

    @Inject
    private TaskIdHolder taskIdHolder;

    @Override
    public void run() {
        taskIdHolder.setTaskId(getAndAssertDirectTaskId());
    }
}
