package io.github.lumnitzf.taskscoped.test.beans;

import io.github.lumnitzf.taskscoped.TaskPreserving;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class ExecutorServiceProducer {

    @Produces
    @ApplicationScoped
    @TaskPreserving
    ExecutorService getExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

    void destroyExecutorService(@Disposes @TaskPreserving ExecutorService service) {
        service.shutdown();
    }
}
