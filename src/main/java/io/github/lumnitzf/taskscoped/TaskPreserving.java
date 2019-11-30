package io.github.lumnitzf.taskscoped;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Qualifier
@Retention(value = RUNTIME)
@Target(value = {TYPE, METHOD, FIELD, PARAMETER})
@Documented
public @interface TaskPreserving {

    class Literal extends AnnotationLiteral<TaskPreserving> implements TaskPreserving {

        public static final Literal INSTANCE = new Literal();

        private Literal() {
        }
    }
}
