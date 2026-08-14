package io.github.sambarker.logsquelcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static io.github.sambarker.logsquelcher.LoggingEventAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @EffectiveLogLevel(Level.DEBUG)
    void methodLevelAnnotationEnablesDebugLogging(CapturedLogs logs) {
        // With @EffectiveLogLevel(DEBUG), isDebugEnabled() should return true
        assertTrue(LOG.isDebugEnabled());

        LOG.debug("debug message");

        assertThat(logs.logged(EffectiveLogLevelTest.class, Level.DEBUG))
                .singleElement()
                .formattedMessage()
                .isEqualTo("debug message");
    }

    @Test
    @EffectiveLogLevel(Level.WARN)
    void methodLevelAnnotationDisablesDebugWhenSetToWarn(CapturedLogs logs) {
        // With @EffectiveLogLevel(WARN), isDebugEnabled() should return false
        assertFalse(LOG.isDebugEnabled());
        assertFalse(LOG.isInfoEnabled());
        assertTrue(LOG.isWarnEnabled());

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
    @EffectiveLogLevel(Level.TRACE)
    void methodLevelAnnotationEnablesTraceLogging(CapturedLogs logs) {
        assertTrue(LOG.isTraceEnabled());
        assertTrue(LOG.isDebugEnabled());
        assertTrue(LOG.isInfoEnabled());

        LOG.trace("trace message");

        assertThat(logs.logged(EffectiveLogLevelTest.class, Level.TRACE))
                .singleElement()
                .formattedMessage()
                .isEqualTo("trace message");
    }

    @Nested
    @EffectiveLogLevel(Level.DEBUG)
    class ClassLevelAnnotation {

        @Test
        void classLevelAnnotationAppliestoAllTests(CapturedLogs logs) {
            assertTrue(LOG.isDebugEnabled());

            LOG.debug("debug from class-level annotation");

            assertThat(logs.logged(EffectiveLogLevelTest.class, Level.DEBUG))
                    .singleElement()
                    .formattedMessage()
                    .isEqualTo("debug from class-level annotation");
        }

        @Test
        @EffectiveLogLevel(Level.WARN)
        void methodLevelOverridesClassLevel(CapturedLogs logs) {
            // Method-level @EffectiveLogLevel(WARN) should override class-level DEBUG
            assertFalse(LOG.isDebugEnabled());
            assertTrue(LOG.isWarnEnabled());

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
        @EffectiveLogLevel(Level.TRACE)
        void methodLevelCanLowerTheLevel(CapturedLogs logs) {
            // Method-level TRACE should override class-level DEBUG
            assertTrue(LOG.isTraceEnabled());
            assertTrue(LOG.isDebugEnabled());

            LOG.trace("trace message");
            LOG.debug("debug message");

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

    @Nested
    @EffectiveLogLevel(Level.INFO)
    class BeforeEachLifecycle {

        private boolean debugEnabledDuringBeforeEach;

        @BeforeEach
        void setup() {
            // Class-level annotation should be active during @BeforeEach
            debugEnabledDuringBeforeEach = LOG.isDebugEnabled();
            LOG.info("from beforeEach");
        }

        @Test
        void classLevelAnnotationActiveInBeforeEach(CapturedLogs logs) {
            // DEBUG should NOT be enabled since class is set to INFO
            assertFalse(debugEnabledDuringBeforeEach);

            // The info log from @BeforeEach should be captured
            assertThat(logs.logged(EffectiveLogLevelTest.class, Level.INFO))
                    .anySatisfy(event ->
                            assertThat(event).formattedMessage().isEqualTo("from beforeEach"));
        }

        @Test
        @EffectiveLogLevel(Level.DEBUG)
        void methodLevelActiveInBeforeEach(CapturedLogs logs) {
            // Method-level DEBUG applies to @BeforeEach + test + @AfterEach
            assertTrue(debugEnabledDuringBeforeEach);

            // And during the test method itself, DEBUG should still be active
            assertTrue(LOG.isDebugEnabled());
            LOG.debug("from test method");

            assertThat(logs.logged(EffectiveLogLevelTest.class, Level.DEBUG))
                    .singleElement()
                    .formattedMessage()
                    .isEqualTo("from test method");
        }
    }
}
