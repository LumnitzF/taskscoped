package io.github.lumnitzf.taskscoped;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.DefinitionException;
import java.util.concurrent.ExecutorService;

class Exceptions {

    private Exceptions() {
    }

    static ContextNotActiveException taskScopeNotActive() {
        return new ContextNotActiveException("TaskScope is not active");
    }

    static DefinitionException beanNotOfTypeExecutorService(Bean<?> bean) {
        return new DefinitionException(bean + " must be of type " + ExecutorService.class);
    }

    static DefinitionException beanNotDependentScoped(Bean<?> bean) {
        return new DefinitionException(bean + " must have scope " + Dependent.class);
    }
}
