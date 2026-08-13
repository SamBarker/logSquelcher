package io.github.sambarker.logsquelcher;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LogSquelcherAssertionTest {

    private static final Logger LOG = LoggerFactory.getLogger(LogSquelcherAssertionTest.class);

    @Test
    void capturedLogsCanBeInjectedAsParameter(CapturedLogs logs) {
        assertNotNull(logs);
    }

    @Test
    void loggedReturnsEventMatchingLoggerAndLevel(CapturedLogs logs) {
        LOG.warn("expected message");

        LoggingEventAssert.assertThat(logs.logged(LogSquelcherAssertionTest.class, Level.WARN).get(0))
                .formattedMessage()
                .isEqualTo("expected message");
    }

    @Test
    void loggedFormatsMessageArguments(CapturedLogs logs) {
        LOG.warn("hello {}", "world");

        LoggingEventAssert.assertThat(logs.logged(LogSquelcherAssertionTest.class, Level.WARN).get(0))
                .formattedMessage()
                .isEqualTo("hello world");
    }

    @Test
    void loggedReturnsEmptyListWhenNoEventsMatchLogger(CapturedLogs logs) {
        LOG.warn("from this class");

        assertThat(logs.logged(String.class, Level.WARN)).isEmpty();
    }

    @Test
    void loggedReturnsEmptyListWhenNoEventsMatchLevel(CapturedLogs logs) {
        LOG.info("info message");

        assertThat(logs.logged(LogSquelcherAssertionTest.class, Level.WARN)).isEmpty();
    }

    @Test
    void loggedReturnsEmptyListWhenNothingCaptured(CapturedLogs logs) {
        assertThat(logs.logged(LogSquelcherAssertionTest.class, Level.WARN)).isEmpty();
    }

    @Test
    void loggedWithoutLevelMatchesAnyLevel(CapturedLogs logs) {
        LOG.debug("debug message");

        LoggingEventAssert.assertThat(logs.logged(LogSquelcherAssertionTest.class).get(0))
                .formattedMessage()
                .isEqualTo("debug message");
    }

    @Test
    void loggedExposeKeyValuePairsFromFluentApi(CapturedLogs logs) {
        LOG.atWarn().addKeyValue("filterName", "myFilterDef").log("Plugin is deprecated");

        var event = LoggingEventAssert.assertThat(logs.logged(LogSquelcherAssertionTest.class, Level.WARN).get(0));
        event.formattedMessage().isEqualTo("Plugin is deprecated");
        event.containsKeyValue("filterName", "myFilterDef");
    }
}
