package io.github.lumnitzf.taskscoped.test.beans;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class ExecutorServiceProducer {

    @Produces
    @Dependent
    ExecutorService getExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

    void destroyExecutorService(@Disposes ExecutorService service) {
        service.shutdown();
    }
}
