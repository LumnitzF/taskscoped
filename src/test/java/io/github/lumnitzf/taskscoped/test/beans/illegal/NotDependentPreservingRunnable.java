package io.github.lumnitzf.taskscoped.test.beans.illegal;

import io.github.lumnitzf.taskscoped.TaskPreserving;

import javax.enterprise.context.RequestScoped;

@RequestScoped
@TaskPreserving
public class NotDependentPreservingRunnable implements Runnable {

    @Override
    public void run() {

    }
}
