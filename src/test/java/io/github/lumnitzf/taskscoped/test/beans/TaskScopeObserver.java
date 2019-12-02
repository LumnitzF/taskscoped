package io.github.lumnitzf.taskscoped.test.beans;

import io.github.lumnitzf.taskscoped.TaskId;
import io.github.lumnitzf.taskscoped.TaskScoped;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class TaskScopeObserver {

    void init(@Observes @Initialized(TaskScoped.class) TaskId task) {
        System.out.println("INIT: TaskId: " + task);
    }

    void destroy(@Observes @Destroyed(TaskScoped.class) TaskId task) {
        System.out.println("DESTROY: TaskId: " + task);
    }
}
