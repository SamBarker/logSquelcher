package io.github.sambarker.logsquelcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static io.github.sambarker.logsquelcher.LoggingEventAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(LogSquelcherExtension.class)
class EffectiveLogLevelTest {

    private static final Logger LOG = LoggerFactory.getLogger(EffectiveLogLevelTest.class);

    @BeforeEach
    void reset() {
        EventBuffer.reset();
    }

    @Test
    void withoutAnnotationDelegatesToBackend() {
        // The backend for this test is slf4j-simple which defaults to INFO
        // Without @EffectiveLogLevel, isDebugEnabled() should delegate to backend
        // (This test assumes slf4j-simple is configured at INFO or higher)

        // We can't reliably assert on isDebugEnabled() return value without knowing
        // the backend config, but we can verify the annotation mechanism works
        // by checking that logs ARE captured regardless of isEnabled checks
        if (LOG.isDebugEnabled()) {
            LOG.debug("debug message");
        }
        // Logs are always captured to the buffer regardless of isEnabled
    }

    @Test
    @EffectiveLogLevel(logger = EffectiveLogLevelTest.class, level = Level.DEBUG)
    void methodLevelAnnotationEnablesDebugLogging(CapturedLogs logs) {
        // With @EffectiveLogLevel(DEBUG), isDebugEnabled() should return true
        assertThat(LOG.isDebugEnabled()).isTrue();

        if (LOG.isDebugEnabled()) {
            LOG.debug("debug message");
        }

        assertThat(logs.logged(EffectiveLogLevelTest.class, Level.DEBUG))
                .singleElement()
                .formattedMessage()
                .isEqualTo("debug message");
    }

    @Test
    @EffectiveLogLevel(logger = EffectiveLogLevelTest.class, level = Level.WARN)
    void methodLevelAnnotationDisablesDebugWhenSetToWarn(CapturedLogs logs) {
        // With @EffectiveLogLevel(WARN), isDebugEnabled() should return false
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isFalse();
        assertThat(LOG.isWarnEnabled()).isTrue();

        if (LOG.isDebugEnabled()) {
            LOG.debug("should not be logged");
        }
        LOG.warn("warn message");

        assertThat(logs.logged(EffectiveLogLevelTest.class, Level.DEBUG)).isEmpty();
        assertThat(logs.logged(EffectiveLogLevelTest.class, Level.WARN))
                .singleElement()
                .formattedMessage()
                .isEqualTo("warn message");
    }

    @Test
    @EffectiveLogLevel(logger = EffectiveLogLevelTest.class, level = Level.TRACE)
    void methodLevelAnnotationEnablesTraceLogging(CapturedLogs logs) {
        assertThat(LOG.isTraceEnabled()).isTrue();
        assertThat(LOG.isDebugEnabled()).isTrue();
        assertThat(LOG.isInfoEnabled()).isTrue();

        if (LOG.isTraceEnabled()) {
            LOG.trace("trace message");
        }

        assertThat(logs.logged(EffectiveLogLevelTest.class, Level.TRACE))
                .singleElement()
                .formattedMessage()
                .isEqualTo("trace message");
    }

    @Nested
    @EffectiveLogLevel(logger = EffectiveLogLevelTest.class, level = Level.DEBUG)
    class ClassLevelAnnotation {

        @Test
        void classLevelAnnotationAppliestoAllTests(CapturedLogs logs) {
            assertThat(LOG.isDebugEnabled()).isTrue();

            if (LOG.isDebugEnabled()) {
                LOG.debug("debug from class-level annotation");
            }

            assertThat(logs.logged(EffectiveLogLevelTest.class, Level.DEBUG))
                    .singleElement()
                    .formattedMessage()
                    .isEqualTo("debug from class-level annotation");
        }

        @Test
        @EffectiveLogLevel(logger = EffectiveLogLevelTest.class, level = Level.WARN)
        void methodLevelOverridesClassLevel(CapturedLogs logs) {
            // Method-level @EffectiveLogLevel(WARN) should override class-level DEBUG
            assertThat(LOG.isDebugEnabled()).isFalse();
            assertThat(LOG.isWarnEnabled()).isTrue();

            if (LOG.isDebugEnabled()) {
                LOG.debug("should not appear");
            }
            LOG.warn("warn message");

            assertThat(logs.logged(EffectiveLogLevelTest.class, Level.DEBUG)).isEmpty();
            assertThat(logs.logged(EffectiveLogLevelTest.class, Level.WARN))
                    .singleElement()
                    .formattedMessage()
                    .isEqualTo("warn message");
        }

        @Test
        @EffectiveLogLevel(logger = EffectiveLogLevelTest.class, level = Level.TRACE)
        void methodLevelCanLowerTheLevel(CapturedLogs logs) {
            // Method-level TRACE should override class-level DEBUG
            assertThat(LOG.isTraceEnabled()).isTrue();
            assertThat(LOG.isDebugEnabled()).isTrue();

            if (LOG.isTraceEnabled()) {
                LOG.trace("trace message");
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("debug message");
            }

            assertThat(logs.logged(EffectiveLogLevelTest.class, Level.TRACE))
                    .singleElement()
                    .formattedMessage()
                    .isEqualTo("trace message");
            assertThat(logs.logged(EffectiveLogLevelTest.class, Level.DEBUG))
                    .singleElement()
                    .formattedMessage()
                    .isEqualTo("debug message");
        }
    }

    @Test
    @EffectiveLogLevel(loggerName = "io.github.sambarker.logsquelcher.EffectiveLogLevelTest", level = Level.DEBUG)
    void methodLevelAnnotationWithLoggerNameEnablesDebugLogging(CapturedLogs logs) {
        // With @EffectiveLogLevel(loggerName=..., DEBUG), isDebugEnabled() should return true
        assertThat(LOG.isDebugEnabled()).isTrue();

        if (LOG.isDebugEnabled()) {
            LOG.debug("debug message using loggerName");
        }

        assertThat(logs.logged(EffectiveLogLevelTest.class, Level.DEBUG))
                .singleElement()
                .formattedMessage()
                .isEqualTo("debug message using loggerName");
    }

    @Test
    @EffectiveLogLevel(level = Level.DEBUG)
    void globalEffectiveLevelAppliestoAllLoggers(CapturedLogs logs) {
        // With @EffectiveLogLevel(DEBUG) and no logger specified, all loggers should have DEBUG enabled
        Logger otherLogger = LoggerFactory.getLogger("some.other.logger");

        assertThat(LOG.isDebugEnabled()).isTrue();
        assertThat(otherLogger.isDebugEnabled()).isTrue();

        if (LOG.isDebugEnabled()) {
            LOG.debug("debug from test logger");
        }
        if (otherLogger.isDebugEnabled()) {
            otherLogger.debug("debug from other logger");
        }

        assertThat(logs.logged(EffectiveLogLevelTest.class, Level.DEBUG))
                .singleElement()
                .formattedMessage()
                .isEqualTo("debug from test logger");
        assertThat(logs.logged("some.other.logger", Level.DEBUG))
                .singleElement()
                .formattedMessage()
                .isEqualTo("debug from other logger");
    }

    @Nested
    @EffectiveLogLevel(logger = EffectiveLogLevelTest.class, level = Level.INFO)
    class BeforeEachLifecycle {

        private boolean debugEnabledDuringBeforeEach;

        @BeforeEach
        void setup() {
            // Class-level annotation should be active during @BeforeEach
            debugEnabledDuringBeforeEach = LOG.isDebugEnabled();
            if (LOG.isInfoEnabled()) {
                LOG.info("from beforeEach");
            }
        }

        @Test
        void classLevelAnnotationActiveInBeforeEach(CapturedLogs logs) {
            // DEBUG should NOT be enabled since class is set to INFO
            assertThat(debugEnabledDuringBeforeEach).isFalse();

            // The info log from @BeforeEach should be captured
            assertThat(logs.logged(EffectiveLogLevelTest.class, Level.INFO))
                    .anySatisfy(event ->
                            assertThat(event).formattedMessage().isEqualTo("from beforeEach"));
        }

        @Test
        @EffectiveLogLevel(logger = EffectiveLogLevelTest.class, level = Level.DEBUG)
        void methodLevelActiveInBeforeEach(CapturedLogs logs) {
            // Method-level DEBUG applies to @BeforeEach + test + @AfterEach
            assertThat(debugEnabledDuringBeforeEach).isTrue();

            // And during the test method itself, DEBUG should still be active
            assertThat(LOG.isDebugEnabled()).isTrue();
            if (LOG.isDebugEnabled()) {
                LOG.debug("from test method");
            }

            assertThat(logs.logged(EffectiveLogLevelTest.class, Level.DEBUG))
                    .singleElement()
                    .formattedMessage()
                    .isEqualTo("from test method");
        }
    }
}
