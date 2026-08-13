package io.github.sambarker.logsquelcher;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.slf4j.event.KeyValuePair;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;
import java.util.Map;

/**
 * AssertJ assertion for {@link LoggingEvent} instances returned by
 * {@link CapturedLogs#logged(Class, Level)} and {@link CapturedLogs#logged(Class)}.
 *
 * <p>Typical usage:
 * <pre>{@code
 * import static io.github.sambarker.logsquelcher.LoggingEventAssert.assertThat;
 *
 * assertThat(ext.logged(MyService.class, Level.WARN))
 *     .hasFormattedMessage("Plugin is deprecated")
 *     .containsKeyValue("filterName", "myFilterDef");
 * }</pre>
 */
public class LoggingEventAssert extends AbstractAssert<LoggingEventAssert, LoggingEvent> {

    private LoggingEventAssert(LoggingEvent actual) {
        super(actual, LoggingEventAssert.class);
    }

    public static LoggingEventAssert assertThat(LoggingEvent actual) {
        return new LoggingEventAssert(actual);
    }

    public static LoggingEventsAssert assertThat(List<LoggingEvent> actual) {
        return new LoggingEventsAssert(actual);
    }

    /**
     * Navigate to AssertJ string assertions on the raw SLF4J message template (placeholders not resolved).
     *
     * <pre>{@code
     * LoggingEventAssert.assertThat(event)
     *     .messageTemplate()
     *     .contains("closing channel");
     * }</pre>
     */
    public AbstractStringAssert<?> messageTemplate() {
        isNotNull();
        return Assertions.assertThat(actual.getMessage()).as("message template");
    }

    /**
     * Navigate to AssertJ string assertions on the SLF4J-formatted message (placeholders resolved).
     *
     * <pre>{@code
     * LoggingEventAssert.assertThat(event)
     *     .formattedMessage()
     *     .isEqualTo("Plugin foo not found");
     * }</pre>
     */
    public AbstractStringAssert<?> formattedMessage() {
        isNotNull();
        String formatted = MessageFormatter.arrayFormat(actual.getMessage(), actual.getArgumentArray(),
                actual.getThrowable()).getMessage();
        return Assertions.assertThat(formatted).as("formatted message");
    }

    /**
     * Verifies that the SLF4J-formatted message (placeholders resolved) equals {@code expected}.
     *
     * @deprecated Use {@link #formattedMessage()}{@code .isEqualTo(expected)} instead.
     */
    @Deprecated(since = "0.3.0", forRemoval = true)
    public LoggingEventAssert hasFormattedMessage(String expected) {
        formattedMessage().isEqualTo(expected);
        return this;
    }

    /**
     * Verifies that the event's key-value pairs contain all entries in {@code expected}.
     */
    public LoggingEventAssert hasKeyValues(Map<String, ?> expected) {
        isNotNull();
        expected.forEach(this::containsKeyValue);
        return this;
    }

    /**
     * Verifies that the event's key-value pairs contain an entry with the given key and value.
     */
    public LoggingEventAssert containsKeyValue(String key, Object value) {
        isNotNull();
        if (actual.getKeyValuePairs() == null) {
            failWithMessage("Expected key-value pairs to contain <%s=%s> but getKeyValuePairs() returned null", key, value);
            return this;
        }
        boolean found = actual.getKeyValuePairs().stream()
                .anyMatch(kv -> key.equals(kv.key) && String.valueOf(value).equals(String.valueOf(kv.value)));
        if (!found) {
            failWithMessage("Expected key-value pairs to contain <%s=%s> but was <%s>",
                    key, value, actual.getKeyValuePairs());
        }
        return this;
    }
}
