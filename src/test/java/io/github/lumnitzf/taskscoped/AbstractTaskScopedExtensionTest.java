package io.github.lumnitzf.taskscoped;

import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;

@EnableWeld
abstract class AbstractTaskScopedExtensionTest {

    @WeldSetup
    WeldInitiator weld = createWeld();

    private WeldInitiator createWeld() {
        final Weld weld = WeldInitiator.createWeld();
        // Add all beans from our "production" package
        weld.addPackage(false, TaskScopedExtension.class);
        // Must be same bean discovery mode as in src/main/resources/META-INF/beans.xml
        weld.setBeanDiscoveryMode(BeanDiscoveryMode.ANNOTATED);
        // Add all beans we want for this test
        getBeanClasses().forEach(weld::addBeanClass);
        // register the extension we want to test
        weld.addExtension(new TaskScopedExtension());
        // allow sub classes to do more customizing
        customizeWeld(weld);
        final WeldInitiator.Builder weldInitiatorBuilder = WeldInitiator.from(weld);
        getActiveScopes().forEach(weldInitiatorBuilder::activate);
        return weldInitiatorBuilder.inject(this).build();
    }

    /**
     * Subclasses may use this method to customize the {@link Weld} for their test execution.
     *
     * @param weld {@link Weld} to customize
     */
    protected void customizeWeld(Weld weld) {
        // Do nothing, but let children override
    }

    /**
     * @return all test beans that should be active
     */
    protected Collection<Class<?>> getBeanClasses() {
        return Collections.emptySet();
    }

    /**
     * @return all scopes that should be active
     * @implNote The {@link javax.enterprise.context.ApplicationScoped ApplicationScope} is always active.
     */
    protected Collection<Class<? extends Annotation>> getActiveScopes() {
        return Collections.emptySet(); // ApplicationScoped is always active
    }
}