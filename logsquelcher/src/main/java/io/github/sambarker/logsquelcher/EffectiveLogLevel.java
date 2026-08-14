package io.github.sambarker.logsquelcher;

import org.slf4j.event.Level;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the effective log level for a specific logger in a test method or class.
 * <p>
 * When present, {@code isXxxEnabled()} methods on the specified logger will respect this level
 * instead of delegating to the backend logger's configured level. This allows tests to
 * capture logs at levels that would otherwise be filtered out by the backend.
 * <p>
 * Without this annotation, {@code isXxxEnabled()} delegates to the backend logger,
 * meaning tests will only capture logs at levels the backend has enabled.
 * <p>
 * <strong>Lifecycle semantics:</strong>
 * <ul>
 *     <li><strong>Method-level:</strong> Applies to that test's entire execution —
 *         {@code @BeforeEach}, test method, and {@code @AfterEach}.</li>
 *     <li><strong>Class-level:</strong> Applies to everything: {@code @BeforeAll}, all test executions,
 *         {@code @AfterAll}.</li>
 *     <li><strong>Method overrides class:</strong> If both are present, the method-level annotation
 *         takes precedence for that test's execution.</li>
 * </ul>
 * <p>
 * Example:
 * <pre>
 * &#64;Test
 * &#64;EffectiveLogLevel(logger = MyService.class, level = Level.DEBUG)
 * void testDebugLogging(CapturedLogs logs) {
 *     // MyService's isDebugEnabled() returns true regardless of backend config
 *     subject.doSomething();
 *
 *     assertThat(logs.logged(MyService.class, Level.DEBUG))
 *         .isNotEmpty();
 * }
 * </pre>
 * <p>
 * The annotation is repeatable to set different levels for multiple loggers:
 * <pre>
 * &#64;EffectiveLogLevel(logger = Foo.class, level = Level.DEBUG)
 * &#64;EffectiveLogLevel(logger = Bar.class, level = Level.TRACE)
 * &#64;Test
 * void testMultipleLoggers(CapturedLogs logs) { ... }
 * </pre>
 * <p>
 * Method-level annotations take precedence over class-level annotations for the same logger.
 */
@Repeatable(EffectiveLevels.class)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EffectiveLogLevel {
    /**
     * The logger class to configure. Required.
     */
    Class<?> logger();

    /**
     * The effective level for this logger. Required.
     */
    Level level();
}
