package io.github.lumnitzf.taskscoped;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class TaskScopedExtension implements Extension {

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
        bbd.addScope(TaskScoped.class, true, false);
    }

    public void processBean(@Observes ProcessBean<?> pb) {
        // TODO: Do correct validation
        // 1. TaskPreserving may only be on Runnable, Callable or ExecutorService. Also check Producers!
        // 2. TaskPreserving Runnable and Callable must have scope Dependent. Also check Producers!
        final Bean<?> bean = pb.getBean();
        if (bean.getQualifiers().contains(TaskPreserving.Literal.INSTANCE)) {
            final Class<?> beanClass = bean.getBeanClass();
            if (!Runnable.class.isAssignableFrom(beanClass) && !Callable.class.isAssignableFrom(beanClass)) {
       //         pb.addDefinitionError(Exceptions.beanNotOfTypeRunnableOrCallable(bean));
            }
            if (!Dependent.class.equals(bean.getScope())) {
         //       pb.addDefinitionError(Exceptions.beanNotDependentScoped(bean));
            }
        }
        // TODO: Check producer
    }

    public void processProducer(ProcessProducer<?, /*TODO: Managed*/ExecutorService> pp) {
        // TODO: Wrap with TaskPreservingDecorator
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd) {
        abd.addContext(new TaskScopedContext());
    }
}
