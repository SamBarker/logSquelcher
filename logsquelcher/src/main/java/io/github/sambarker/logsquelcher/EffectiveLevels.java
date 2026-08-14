package io.github.sambarker.logsquelcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for repeated {@link EffectiveLogLevel} annotations.
 * <p>
 * You don't use this directly - just repeat {@code @EffectiveLogLevel}:
 * <pre>
 * &#64;EffectiveLogLevel(logger = Foo.class, level = Level.DEBUG)
 * &#64;EffectiveLogLevel(logger = Bar.class, level = Level.TRACE)
 * &#64;Test
 * void test() { ... }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EffectiveLevels {
    EffectiveLogLevel[] value();
}
