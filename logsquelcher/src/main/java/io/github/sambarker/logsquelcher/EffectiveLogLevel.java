package io.github.sambarker.logsquelcher;

import org.slf4j.event.Level;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the effective log level for a test method or class.
 * <p>
 * When present, {@code isXxxEnabled()} methods on SLF4J loggers will respect this level
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
 * &#64;EffectiveLogLevel(Level.DEBUG)
 * void testDebugLogging(CapturedLogs logs) {
 *     // isDebugEnabled() returns true regardless of backend config
 *     subject.doSomething();
 *
 *     assertThat(logs.logged(MyService.class, Level.DEBUG))
 *         .isNotEmpty();
 * }
 * </pre>
 * <p>
 * Method-level annotations take precedence over class-level annotations.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EffectiveLogLevel {
    Level value();
}
