package io.github.lumnitzf.taskscoped;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;
import javax.interceptor.Interceptors;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

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
            if (isTaskScopeEnabledIntercepted(pb.getAnnotated())) {
                pb.addDefinitionError(Exceptions.beanMustNotBeInterceptedWithTaskScopeEnabled(bean));
            }
        }
    }

    private boolean isTaskScopeEnabledIntercepted(Annotated annotated) {
        return annotated.isAnnotationPresent(TaskScopeEnabled.class) ||
                annotated.getAnnotations().stream().filter(a -> a instanceof Interceptors).map(Interceptors.class::cast)
                        .flatMap(
                                i -> Stream.of(i.value())).anyMatch(TaskScopeEnabled.class::equals);
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd) {
        abd.addContext(new TaskScopedContext());
    }
}
