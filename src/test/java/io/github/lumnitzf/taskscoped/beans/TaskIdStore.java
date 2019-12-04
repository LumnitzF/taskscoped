package io.github.lumnitzf.taskscoped.beans;

import io.github.lumnitzf.taskscoped.TaskId;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TaskIdStore {

    private TaskId taskId;

    public void setTaskId(TaskId taskId) {
        this.taskId = taskId;
    }

    public TaskId getTaskId() {
        return taskId;
    }
}
