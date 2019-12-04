package io.github.lumnitzf.taskscoped;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * Interceptor implementation for {@link TaskScopeEnabled}.
 *
 * @author Fritz Lumnitz
 */
@TaskScopeEnabled
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_AFTER)
class TaskScopeEnabledInterceptor {

    @Inject
    private BeanManager beanManager;

    @AroundInvoke
    public Object invoke(InvocationContext invocation) throws Exception {
        TaskScopedContext.activate();
        final TaskScopedContext context = (TaskScopedContext) beanManager.getContext(TaskScoped.class);
        final TaskId previous = context.enter();
        try {
            return invocation.proceed();
        } finally {
            context.exit(previous);
        }
    }

}
