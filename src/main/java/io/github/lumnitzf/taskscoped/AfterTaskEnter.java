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

/**
 * <p>
 * The {@code @AfterTaskEnter} qualifier.
 * </p>
 *
 * @author Fritz Lumnitz
 */
@Qualifier
@Target({TYPE, METHOD, PARAMETER, FIELD})
@Retention(RUNTIME)
@Documented
public @interface AfterTaskEnter {
    /**
     * Supports inline instantiation of the {@link AfterTaskEnter} qualifier.
     */
    class Literal extends AnnotationLiteral<AfterTaskEnter> implements AfterTaskEnter {

        public static final Literal INSTANCE = new Literal();

        private Literal() {
        }
    }

}
