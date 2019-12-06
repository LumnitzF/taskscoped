package io.github.lumnitzf.taskscoped.beans;

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
    ExecutorService getDefaultService() {
        return Executors.newSingleThreadExecutor();
    }

    @Produces
    @ApplicationScoped
    @TaskPreserving
    ExecutorService getExecutorService(ExecutorService defaultService) {
        return defaultService;
    }

    void destroyExecutorService(@Disposes ExecutorService service) {
        service.shutdown();
    }
}