package io.github.lumnitzf.taskscoped.test;

import io.github.lumnitzf.taskscoped.TaskScopedExtension;
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;

import java.lang.annotation.Annotation;
import java.util.Collection;

@EnableWeld
abstract class AbstractTaskScopedExtensionTest {

    @WeldSetup
    public WeldInitiator weld = createWeld();

    private WeldInitiator createWeld() {
        final Weld weld = WeldInitiator.createWeld();
        weld.addPackage(false, TaskScopedExtension.class);
        weld.setBeanDiscoveryMode(BeanDiscoveryMode.ANNOTATED);
        getBeanClasses().forEach(weld::addBeanClass);
        weld.addExtension(new TaskScopedExtension());
        customizeWeld(weld);
        final WeldInitiator.Builder weldInitiatorBuilder = WeldInitiator.from(weld);
        getActiveScopes().forEach(weldInitiatorBuilder::activate);
        return weldInitiatorBuilder.build();
    }

    protected void customizeWeld(Weld weld) {
        // Do nothing, but let children override
    }

    abstract Collection<Class<?>> getBeanClasses();

    abstract Collection<Class<? extends Annotation>> getActiveScopes();
}