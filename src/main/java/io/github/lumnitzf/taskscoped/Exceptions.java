package io.github.lumnitzf.taskscoped;

import javax.enterprise.context.ContextNotActiveException;

/**
 * Utility class for creating common exceptions.
 *
 * @author Fritz Lumnitz
 */
class Exceptions {

    private Exceptions() {
    }

    /**
     * @return A {@link ContextNotActiveException} with message that the TaskScope is not active
     */
    static ContextNotActiveException taskScopeNotActive() {
        return new ContextNotActiveException("TaskScope is not active");
    }
}
