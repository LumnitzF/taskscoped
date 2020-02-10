package io.github.lumnitzf.taskscoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(TaskScopedExtension.class);

    void beforeBeanDiscovery(@Observes final BeforeBeanDiscovery bbd) {
        LOG.info("Registering task scope");
        bbd.addScope(TaskScoped.class, true, false);
    }

    void processExecutorServiceProducer(@Observes final ProcessProducer<?, ExecutorService> pp, final BeanManager beanManager) {
        if (pp.getAnnotatedMember().isAnnotationPresent(TaskPreserving.class)) {
            final Producer<ExecutorService> producer = pp.getProducer();
            LOG.info("Adding task preserving capability to {}", producer);
            pp.setProducer(new DelegateProducer<>(producer,
                    delegate -> new TaskPreservingExecutorServiceDecorator(beanManager, delegate)));
        }
    }

    void processManagedExecutorServiceProducer(@Observes final ProcessProducer<?, ManagedExecutorService> pp,
                                               final BeanManager beanManager) {
        if (pp.getAnnotatedMember().isAnnotationPresent(TaskPreserving.class)) {
            final Producer<ManagedExecutorService> producer = pp.getProducer();
            LOG.info("Adding task preserving capability to {}", producer);
            pp.setProducer(new DelegateProducer<>(producer,
                    delegate -> new TaskPreservingManagedExecutorServiceDecorator(beanManager, delegate)));
        }
    }

    void afterBeanDiscovery(@Observes final AfterBeanDiscovery abd, final BeanManager beanManager) {
        LOG.info("Adding TaskScopedContext");
        abd.addContext(new TaskScopedContext(beanManager));
    }

    private static class DelegateProducer<X> implements Producer<X> {

        private final Producer<X> delegate;
        private final Function<X, X> producerWrapper;

        private DelegateProducer(final Producer<X> delegate, final Function<X, X> producerWrapper) {
            this.delegate = delegate;
            this.producerWrapper = producerWrapper;
        }

        @Override
        public X produce(final CreationalContext<X> ctx) {
            // Dependent producers may return null, so we also return null instead of creating the delegate (which will throw an exception)
            return Optional.ofNullable(delegate.produce(ctx)).map(producerWrapper).orElse(null);
        }

        @Override
        public void dispose(final X instance) {
            delegate.dispose(instance);
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return delegate.getInjectionPoints();
        }
    }
}
