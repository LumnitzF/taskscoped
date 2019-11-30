package io.github.lumnitzf.taskscoped.test.beans;

import io.github.lumnitzf.taskscoped.TaskId;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TaskIdHolder {

    private TaskId taskId;

    public TaskId getTaskId() {
        return taskId;
    }

    public void setTaskId(TaskId taskId) {
        this.taskId = taskId;
    }
}
