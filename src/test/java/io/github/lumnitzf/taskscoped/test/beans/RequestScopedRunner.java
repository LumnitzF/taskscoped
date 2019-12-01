package io.github.lumnitzf.taskscoped.test.beans;

import io.github.lumnitzf.taskscoped.TaskId;
import io.github.lumnitzf.taskscoped.TaskPreserving;
import io.github.lumnitzf.taskscoped.TaskScopeEnabled;
import org.junit.jupiter.api.Assertions;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@RequestScoped
@TaskScopeEnabled
public class RequestScopedRunner {

    @Inject
    private TaskPreservingRunnable runnable;

    @Inject
    private TaskId taskId;

    @Inject
    private TaskIdHolder holder;

    @Inject
    @TaskPreserving
    private ExecutorService service;

    public void doRun() throws ExecutionException, InterruptedException {
        Future<?> f = service.submit(runnable);
        f.get();
        Assertions.assertEquals(taskId.getValue(), holder.getTaskId().getValue());
    }
}
