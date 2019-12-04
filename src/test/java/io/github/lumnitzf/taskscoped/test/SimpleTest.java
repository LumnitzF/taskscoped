package io.github.lumnitzf.taskscoped.test;

import io.github.lumnitzf.taskscoped.test.beans.ExecutorServiceProducer;
import io.github.lumnitzf.taskscoped.test.beans.RequestScopedRunner;
import io.github.lumnitzf.taskscoped.test.beans.TaskIdHolder;
import io.github.lumnitzf.taskscoped.test.beans.TaskPreservingRunnable;
import io.github.lumnitzf.taskscoped.test.beans.TaskScopeObserver;
import org.junit.jupiter.api.Test;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

class SimpleTest extends AbstractTaskScopedExtensionTest {
    @Inject
    private RequestScopedRunner runner;

    @Inject
    private ExecutorService executorService;

    @Override
    Collection<Class<?>> getBeanClasses() {
        return Arrays.asList(TaskIdHolder.class, TaskPreservingRunnable.class, RequestScopedRunner.class,
                ExecutorServiceProducer.class, TaskScopeObserver.class);
    }

    @Override
    Collection<Class<? extends Annotation>> getActiveScopes() {
        return Arrays.asList(ApplicationScoped.class, RequestScoped.class);
    }

    @Test
    void test() throws Exception {
        // Trigger executorService creation before entering the TaskScope
        executorService.toString();
        executorService.submit(() -> {});
        runner.doRun();
    }
}
