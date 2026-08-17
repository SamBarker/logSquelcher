package io.github.sambarker.logsquelcher;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;

import java.util.List;

/**
 * Per-test view of the logs captured by logSquelcher.
 *
 * <p>Inject it as a test parameter to make assertions about log output:
 * <pre>{@code
 * @Test
 * void myTest(CapturedLogs logs) {
 *     myService.doSomething();
 *     LoggingEventAssert.assertThat(logs.logged(MyService.class, Level.WARN))
 *         .singleElement()
 *         .formattedMessage()
 *         .isEqualTo("something went wrong");
 * }
 * }</pre>
 */
public class CapturedLogs implements ExtensionContext.Store.CloseableResource, AutoCloseable {

    private final long startNanos;

    CapturedLogs(long startNanos) {
        this.startNanos = startNanos;
    }

    long startNanos() {
        return startNanos;
    }

    /**
     * Returns all log events from {@code logger} at {@code level} captured since this test started.
     * Returns an empty list if none were captured.
     *
     * <p>The window covers all threads — not just the JUnit test thread.
     *
     * @param logger the logger class whose events to inspect
     * @param level  the required log level
     * @return all matching events, in capture order; never null
     */
    public List<LoggingEvent> logged(Class<?> logger, Level level) {
        return EventBuffer.extractWindow(startNanos, System.nanoTime()).stream()
                .map(CapturedEvent::loggingEvent)
                .filter(e -> logger.getName().equals(e.getLoggerName()))
                .filter(e -> level == e.getLevel())
                .toList();
    }

    /**
     * Returns all log events from the logger named {@code loggerName} at {@code level} captured since this test started.
     * Returns an empty list if none were captured.
     *
     * <p>The window covers all threads — not just the JUnit test thread.
     *
     * @param loggerName the logger name whose events to inspect
     * @param level      the required log level
     * @return all matching events, in capture order; never null
     */
    public List<LoggingEvent> logged(String loggerName, Level level) {
        return EventBuffer.extractWindow(startNanos, System.nanoTime()).stream()
                .map(CapturedEvent::loggingEvent)
                .filter(e -> loggerName.equals(e.getLoggerName()))
                .filter(e -> level == e.getLevel())
                .toList();
    }

    /**
     * Returns all log events from {@code logger} at any level captured since this test started.
     * Returns an empty list if none were captured.
     *
     * <p>Convenience overload of {@link #logged(Class, Level)} that matches any level.
     *
     * @param logger the logger class whose events to inspect
     * @return all matching events, in capture order; never null
     */
    public List<LoggingEvent> logged(Class<?> logger) {
        return EventBuffer.extractWindow(startNanos, System.nanoTime()).stream()
                .map(CapturedEvent::loggingEvent)
                .filter(e -> logger.getName().equals(e.getLoggerName()))
                .toList();
    }

    /**
     * Returns all log events captured since this test started, regardless of logger or level.
     * Returns an empty list if none were captured.
     *
     * <p>The window covers all threads — not just the JUnit test thread.
     *
     * @return all captured events, in capture order; never null
     */
    public List<LoggingEvent> logged() {
        return EventBuffer.extractWindow(startNanos, System.nanoTime()).stream()
                .map(CapturedEvent::loggingEvent)
                .toList();
    }

    @Override
    public void close() {
        // EventBuffer is global; nothing per-test to release here.
    }
}
