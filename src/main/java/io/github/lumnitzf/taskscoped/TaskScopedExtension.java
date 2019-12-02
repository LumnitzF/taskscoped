package io.github.lumnitzf.taskscoped;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Extension providing the behavior defined by {@link TaskScoped} and {@link TaskPreserving}.
 *
 * @author Fritz Lumnitz
 */
public class TaskScopedExtension implements Extension {

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
        bbd.addScope(TaskScoped.class, true, false);
    }

    void processExecutorServiceProducer(@Observes ProcessProducer<?, ExecutorService> pp, BeanManager beanManager) {
        if (pp.getAnnotatedMember().isAnnotationPresent(TaskPreserving.class)) {
            pp.setProducer(new DelegateProducer<>(pp.getProducer(),
                    delegate -> new TaskPreservingExecutorServiceDecorator(beanManager, delegate)));
        }
    }

    void processManagedExecutorServiceProducer(@Observes ProcessProducer<?, ManagedExecutorService> pp,
                                               BeanManager beanManager) {
        if (pp.getAnnotatedMember().isAnnotationPresent(TaskPreserving.class)) {
            pp.setProducer(new DelegateProducer<>(pp.getProducer(),
                    delegate -> new TaskPreservingManagedExecutorServiceDecorator(beanManager, delegate)));
        }
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery abd) {
        abd.addContext(new TaskScopedContext());
    }

    private static class DelegateProducer<X> implements Producer<X> {

        private final Producer<X> delegate;
        private final Function<X, X> producerWrapper;

        private DelegateProducer(Producer<X> delegate, Function<X, X> producerWrapper) {
            this.delegate = delegate;
            this.producerWrapper = producerWrapper;
        }

        @Override
        public X produce(CreationalContext<X> ctx) {
            // Dependent producers may return null, so we also return null instead of creating the delegate (which will throw an exception)
            return Optional.ofNullable(delegate.produce(ctx)).map(producerWrapper).orElse(null);
        }

        @Override
        public void dispose(X instance) {
            delegate.dispose(instance);
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return delegate.getInjectionPoints();
        }
    }
}
