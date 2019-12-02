package io.github.lumnitzf.taskscoped;

import javax.enterprise.context.ContextNotActiveException;

class Exceptions {

    private Exceptions() {
    }

    static ContextNotActiveException taskScopeNotActive() {
        return new ContextNotActiveException("TaskScope is not active");
    }
}
