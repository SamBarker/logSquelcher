package io.github.sambarker.logsquelcher;

import org.junit.jupiter.api.extension.ExtendWith;
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
 * Example using logger class:
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
 * Example using logger name (useful for third-party loggers):
 * <pre>
 * &#64;Test
 * &#64;EffectiveLogLevel(loggerName = "org.apache.kafka.clients", level = Level.DEBUG)
 * void testKafkaClientLogging(CapturedLogs logs) { ... }
 * </pre>
 * <p>
 * Example setting level globally (affects all loggers via ROOT logger):
 * <pre>
 * &#64;Test
 * &#64;EffectiveLogLevel(level = Level.DEBUG)
 * void testWithGlobalDebug(CapturedLogs logs) {
 *     // All loggers have DEBUG enabled, regardless of backend config
 * }
 * </pre>
 * <p>
 * The annotation is repeatable to set different levels for multiple loggers:
 * <pre>
 * &#64;EffectiveLogLevel(logger = Foo.class, level = Level.DEBUG)
 * &#64;EffectiveLogLevel(loggerName = "com.example.third.party", level = Level.TRACE)
 * &#64;Test
 * void testMultipleLoggers(CapturedLogs logs) { ... }
 * </pre>
 * <p>
 * Method-level annotations take precedence over class-level annotations for the same logger.
 * <p>
 * This annotation is meta-annotated with {@link ExtendWith @ExtendWith(LogSquelcherExtension.class)},
 * so applying it registers the extension automatically — no explicit {@code @ExtendWith} or
 * extension autodetection is required.
 */
@Repeatable(EffectiveLevels.class)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(LogSquelcherExtension.class)
public @interface EffectiveLogLevel {
    /**
     * The logger class to configure. Mutually exclusive with {@link #loggerName()}.
     * <p>
     * If neither {@code logger} nor {@code loggerName} is specified, the level is set on the
     * ROOT logger, which typically affects all loggers in the system.
     */
    Class<?> logger() default void.class;

    /**
     * The logger name to configure. Mutually exclusive with {@link #logger()}.
     * <p>
     * If neither {@code logger} nor {@code loggerName} is specified, the level is set on the
     * ROOT logger, which typically affects all loggers in the system.
     */
    String loggerName() default "";

    /**
     * The effective level for this logger. Required.
     */
    Level level();
}
