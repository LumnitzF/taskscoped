package io.github.lumnitzf.taskscoped;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@TaskScopeEnabled
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class TaskScopeEnabledInterceptor {

    @Inject
    private BeanManager beanManager;

    @AroundInvoke
    public Object invoke(InvocationContext invocation) throws Exception {
        final TaskScopedContext context = (TaskScopedContext) beanManager.getContext(TaskScoped.class);
        final TaskId previous = context.enter();
        try {
            return invocation.proceed();
        } finally {
            context.exit(previous);
        }
    }

}
