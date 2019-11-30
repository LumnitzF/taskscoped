package io.github.lumnitzf.taskscoped;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.DefinitionException;
import java.util.concurrent.Callable;

class Exceptions {

    private Exceptions() {
    }

    static ContextNotActiveException taskScopeNotActive() {
        return new ContextNotActiveException("TaskScope is not active");
    }

    static DefinitionException beanNotOfTypeRunnableOrCallable(Bean<?> bean) {
        return new DefinitionException(bean + " must be of type " + Runnable.class + " or " + Callable.class);
    }

    static DefinitionException beanNotDependentScoped(Bean<?> bean) {
        return new DefinitionException(bean + " must have scope " + Dependent.class);
    }
}
