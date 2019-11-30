package io.github.lumnitzf.taskscoped;

import javax.enterprise.context.NormalScope;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Documented
@NormalScope
@Inherited
public @interface TaskScoped {

    class Literal extends AnnotationLiteral<TaskScoped> implements TaskScoped {

        public static final Literal INSTANCE = new Literal();

        private Literal() {
        }
    }
}




