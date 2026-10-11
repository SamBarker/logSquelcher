package io.github.sambarker.logsquelcher;

import org.junit.jupiter.api.extension.ExtendWith;

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
 * <p>
 * Meta-annotated with {@link ExtendWith @ExtendWith(LogSquelcherExtension.class)} so that the
 * repeated form self-registers the extension: when {@code @EffectiveLogLevel} is repeated, only
 * this container is directly present on the element, so the meta-annotation must live here too.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(LogSquelcherExtension.class)
public @interface EffectiveLevels {
    EffectiveLogLevel[] value();
}
