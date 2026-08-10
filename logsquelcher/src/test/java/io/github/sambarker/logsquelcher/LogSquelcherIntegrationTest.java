package io.github.sambarker.logsquelcher;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.sambarker.logsquelcher.fixture.BeforeAllLoggingFixture;
import io.github.sambarker.logsquelcher.fixture.ConcurrentFailingFixture;
import io.github.sambarker.logsquelcher.fixture.LoggingFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

class LogSquelcherIntegrationTest {

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        appender = new ListAppender<>();
        logbackContext().ifPresent(ctx -> {
            appender.setContext(ctx);
            appender.start();
            ctx.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
        });
    }

    @AfterEach
    void detachAppender() {
        logbackContext().ifPresent(ctx ->
                ctx.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(appender));
    }

    @Test
    void logsFromFailingTestAreReplayedToOutput() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectMethod(LoggingFixture.class, "failingTestThatLogs"))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).failed(1));

        assertThat(appender.list)
                .anyMatch(e -> e.getFormattedMessage().contains(LoggingFixture.REPLAYED_MESSAGE));
    }

    @Test
    void logsFromPassingTestAreSuppressed() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectMethod(LoggingFixture.class, "passingTestThatLogs"))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1));

        assertThat(appender.list)
                .noneMatch(e -> e.getFormattedMessage().contains(LoggingFixture.SUPPRESSED_MESSAGE));
    }

    @Test
    void logsFromBeforeAllAreReplayedOnFailure() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(BeforeAllLoggingFixture.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).failed(1));

        assertThat(appender.list)
                .anyMatch(e -> e.getFormattedMessage().contains(BeforeAllLoggingFixture.BEFORE_ALL_MESSAGE));
    }

    @Test
    void replayIsSuppressedInConcurrentExecutionMode() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(ConcurrentFailingFixture.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).failed(1));

        assertThat(appender.list)
                .noneMatch(e -> e.getFormattedMessage().contains(ConcurrentFailingFixture.SUPPRESSED_MESSAGE));
    }

    @Test
    void replayFallsBackToLevelCallForNonLoggingEventAwareBackend() {
        AtomicReference<Level> capturedLevel = new AtomicReference<>();
        AtomicReference<String> capturedMsg = new AtomicReference<>();

        ILoggerFactory nonAwareDelegate = name -> new AbstractLogger() {
            {
                this.name = name;
            }

            @Override
            protected void handleNormalizedLoggingCall(Level level, Marker marker,
                    String msg, Object[] args, Throwable t) {
                capturedLevel.set(level);
                capturedMsg.set(MessageFormatter.arrayFormat(msg, args, t).getMessage());
            }

            @Override public String getFullyQualifiedCallerName() { return null; }
            @Override public boolean isTraceEnabled() { return true; }
            @Override public boolean isTraceEnabled(Marker m) { return true; }
            @Override public boolean isDebugEnabled() { return true; }
            @Override public boolean isDebugEnabled(Marker m) { return true; }
            @Override public boolean isInfoEnabled() { return true; }
            @Override public boolean isInfoEnabled(Marker m) { return true; }
            @Override public boolean isWarnEnabled() { return true; }
            @Override public boolean isWarnEnabled(Marker m) { return true; }
            @Override public boolean isErrorEnabled() { return true; }
            @Override public boolean isErrorEnabled(Marker m) { return true; }
        };

        var factory = new CapturingLoggerFactory(nonAwareDelegate);
        factory.replay(new LogSquelcherLoggingEvent(Level.WARN, "com.example.Foo",
                "hello {}", new Object[]{"world"}, null,
                System.currentTimeMillis(), "main", null));

        assertThat(capturedLevel.get()).isEqualTo(Level.WARN);
        assertThat(capturedMsg.get()).isEqualTo("hello world");
    }

    private java.util.Optional<LoggerContext> logbackContext() {
        if (LoggerFactory.getILoggerFactory() instanceof CapturingLoggerFactory capturing
                && capturing.getDelegate() instanceof LoggerContext ctx) {
            return java.util.Optional.of(ctx);
        }
        return java.util.Optional.empty();
    }
}
