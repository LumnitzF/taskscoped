package io.github.lumnitzf.taskscoped;

import javax.enterprise.util.AnnotationLiteral;
import javax.interceptor.InterceptorBinding;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE, METHOD})
@Retention(RUNTIME)
@Documented
@InterceptorBinding
@Inherited
public @interface TaskScopeEnabled {

    class Literal extends AnnotationLiteral<TaskScopeEnabled> implements TaskScopeEnabled {

        public static final Literal INSTANCE = new Literal();

        private Literal() {
        }
    }
}

