package io.github.lumnitzf.taskscoped.test.beans;

import io.github.lumnitzf.taskscoped.TaskId;
import io.github.lumnitzf.taskscoped.TaskIdManager;
import org.junit.jupiter.api.Assertions;

import javax.inject.Inject;

public class BaseTaskPreserving {

    @Inject
    private TaskId taskId;

    @Inject
    private TaskIdManager taskIdManager;

    protected TaskId getAndAssertDirectTaskId() {
        final TaskId directTaskId = taskIdManager.getId();
        Assertions.assertEquals(TaskId.class, directTaskId.getClass());
        Assertions.assertNotEquals(TaskId.class, taskId.getClass());
        Assertions.assertEquals(directTaskId.getValue(), taskId.getValue());
        return directTaskId;
    }

}
