package io.github.lumnitzf.taskscoped;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;
import java.util.concurrent.Callable;

public class TaskScopedExtension implements Extension {

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
        bbd.addScope(TaskScoped.class, true, false);
    }

    public void processBean(@Observes ProcessBean<?> pb) {
        final Bean<?> bean = pb.getBean();
        if (bean.getQualifiers().contains(TaskPreserving.Literal.INSTANCE)) {
            final Class<?> beanClass = bean.getBeanClass();
            if (!Runnable.class.isAssignableFrom(beanClass) && !Callable.class.isAssignableFrom(beanClass)) {
                pb.addDefinitionError(Exceptions.beanNotOfTypeRunnableOrCallable(bean));
            }
            if (!Dependent.class.equals(bean.getScope())) {
                pb.addDefinitionError(Exceptions.beanNotDependentScoped(bean));
            }
        }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd) {
        abd.addContext(new TaskScopedContext());
    }
}
