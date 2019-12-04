package io.github.lumnitzf.taskscoped;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.inject.Produces;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.ExecutorService;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Task preserving {@link Qualifier qualifier}.<br>
 * Task preserving beans execute or are executed in the same TaskScope as the caller, even over multiple threads.
 * <p>
 * A decorator is already provided for {@link ExecutorService} and {@link ManagedExecutorService} container created
 * beans and all instances returned by {@link Produces producers}. To apply the decorator annotate the bean class or the
 * producer.
 * </p>
 *
 * @author Fritz Lumnitz
 * @implNote The default behavior expects every Runnable or Callable to be executed exactly once. Otherwise the moment
 * the TaskScope should be destroyed cannot be determined. In case a different behavior is desired, the application is
 * required to implement its own decorator, which registers and unregisters the tasks correctly.
 */
@Qualifier
@Retention(value = RUNTIME)
@Target(value = {TYPE, METHOD, FIELD, PARAMETER})
@Documented
public @interface TaskPreserving {

    /**
     * Supports inline instantiation of the {@link TaskPreserving} qualifier.
     *
     * @author Fritz Lumnitz
     */
    class Literal extends AnnotationLiteral<TaskPreserving> implements TaskPreserving {

        public static final Literal INSTANCE = new Literal();

        private Literal() {
        }
    }
}
