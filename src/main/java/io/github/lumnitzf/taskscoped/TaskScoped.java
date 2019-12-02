package io.github.lumnitzf.taskscoped;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.NormalScope;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * Specifies that a bean is task scoped.
 * </p>
 * <p>
 * The task scope is active:
 * </p>
 *
 * <ul>
 * <li>during {@link TaskScopeEnabled} invocations</li>
 * <li>during invocations of {@link TaskPreserving} beans</li>
 * <li>during invocations of {@link Runnable} or {@link Callable} inside a Thread managed by a TaskPreserving
 * {@link ExecutorService} or {@link ManagedExecutorService}</li>
 * </ul>
 *
 * <p>
 * The task context is destroyed after each bean exits the {@link TaskScopeEnabled} invocation and all associated
 * {@link TaskPreserving} beans also exit their intercepted invocations.
 * </p>
 *
 * <p>
 * An event with qualifier <tt>@Initialized(TaskScoped.class)</tt> is fired when the task context is initialized and an
 * event with qualifier <tt>@Destroyed(TaskScoped.class)</tt> when the task context is destroyed. The event payload is
 * the respective {@link TaskId}.
 * </p>
 *
 * @author Fritz Lumnitz
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Documented
@NormalScope
@Inherited
public @interface TaskScoped {

    /**
     * Supports inline instantiation of the {@link TaskScoped} qualifier.
     */
    class Literal extends AnnotationLiteral<TaskScoped> implements TaskScoped {

        public static final Literal INSTANCE = new Literal();

        private Literal() {
        }
    }
}




