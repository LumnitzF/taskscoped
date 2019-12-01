package io.github.lumnitzf.taskscoped.test.beans;

import io.github.lumnitzf.taskscoped.TaskPreserving;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import java.util.concurrent.ExecutorService;

@ApplicationScoped
public class ExecutorServiceProducer {

    @Produces
    @ApplicationScoped
    @TaskPreserving
    ManagedExecutorService getExecutorService() {
        return null;
    }

    void destroyExecutorService(@Disposes @TaskPreserving ExecutorService service) {
        service.shutdown();
    }
}
