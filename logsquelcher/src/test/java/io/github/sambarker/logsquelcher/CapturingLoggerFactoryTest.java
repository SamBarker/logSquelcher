package io.github.sambarker.logsquelcher;

import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.spi.LoggingEventAware;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CapturingLoggerFactoryTest {

    @Test
    void replayForwardsEventToRealBackendLogger() {
        // Given
        List<LoggingEvent> received = new ArrayList<>();
        ILoggerFactory delegate = name -> new StubBackendLogger(name, received);
        CapturingLoggerFactory factory = new CapturingLoggerFactory(delegate);
        LoggingEvent event = new LogSquelcherLoggingEvent(Level.WARN, "com.example.Foo", "hello", null, null,
                System.currentTimeMillis(), "main", null);

        // When
        factory.replay(event);

        // Then
        assertEquals(1, received.size());
        assertEquals("hello", received.get(0).getMessage());
    }

    @Test
    void replaySkipsEventWhenBackendLevelDisablesIt() {
        // Given
        List<LoggingEvent> received = new ArrayList<>();
        ILoggerFactory delegate = name -> new WarnOnlyStubLogger(name, received);
        CapturingLoggerFactory factory = new CapturingLoggerFactory(delegate);
        LoggingEvent infoEvent = new LogSquelcherLoggingEvent(Level.INFO, "com.example.Foo", "filtered", null, null,
                System.currentTimeMillis(), "main", null);

        // When
        factory.replay(infoEvent);

        // Then
        assertTrue(received.isEmpty(), "INFO event should not be forwarded to a WARN-only backend");
    }

    @Test
    void replayIsNoOpWhenNoDelegatePresent() {
        // Given
        CapturingLoggerFactory factory = new CapturingLoggerFactory(null);
        LoggingEvent event = new LogSquelcherLoggingEvent(Level.WARN, "com.example.Foo", "hello", null, null,
                System.currentTimeMillis(), "main", null);

        // When / Then
        assertDoesNotThrow(() -> factory.replay(event));
    }

    @Test
    void replayIsNoOpWhenRealLoggerIsNotLoggingEventAware() {
        // Given
        ILoggerFactory delegate = name -> new NonLoggingEventAwareLogger(name);
        CapturingLoggerFactory factory = new CapturingLoggerFactory(delegate);
        LoggingEvent event = new LogSquelcherLoggingEvent(Level.WARN, "com.example.Foo", "hello", null, null,
                System.currentTimeMillis(), "main", null);

        // When / Then
        assertDoesNotThrow(() -> factory.replay(event));
    }

    @Test
    void isEnabledDelegatesToRealLogger() {
        List<LoggingEvent> received = new ArrayList<>();
        ILoggerFactory delegate = name -> new WarnOnlyStubLogger(name, received);
        CapturingLoggerFactory factory = new CapturingLoggerFactory(delegate);
        var logger = factory.getLogger("com.example.Foo");

        assertFalse(logger.isTraceEnabled());
        assertFalse(logger.isDebugEnabled());
        assertFalse(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());
    }

    @Test
    void isEnabledReturnsTrueWhenNoDelegatePresent() {
        CapturingLoggerFactory factory = new CapturingLoggerFactory(null);
        var logger = factory.getLogger("com.example.Foo");

        assertTrue(logger.isTraceEnabled());
        assertTrue(logger.isDebugEnabled());
        assertTrue(logger.isWarnEnabled());
    }

    @Test
    void getLoggerReturnsSeparateCapturingLoggerPerName() {
        // Given
        CapturingLoggerFactory factory = new CapturingLoggerFactory(null);

        // When
        var a = factory.getLogger("a");
        var b = factory.getLogger("b");
        var a2 = factory.getLogger("a");

        // Then
        assertNotSame(a, b);
        assertSame(a, a2);
    }

    // --- test doubles ---

    private static class StubBackendLogger extends AbstractLogger implements LoggingEventAware {
        private final List<LoggingEvent> received;

        StubBackendLogger(String name, List<LoggingEvent> received) {
            this.name = name;
            this.received = received;
        }

        @Override
        public void log(LoggingEvent event) {
            received.add(event);
        }

        @Override
        protected void handleNormalizedLoggingCall(Level level, Marker marker,
                String msg, Object[] args, Throwable t) {
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
    }

    private static class WarnOnlyStubLogger extends AbstractLogger implements LoggingEventAware {
        private final List<LoggingEvent> received;

        WarnOnlyStubLogger(String name, List<LoggingEvent> received) {
            this.name = name;
            this.received = received;
        }

        @Override
        public void log(LoggingEvent event) {
            received.add(event);
        }

        @Override
        protected void handleNormalizedLoggingCall(Level level, Marker marker,
                String msg, Object[] args, Throwable t) {
        }

        @Override public String getFullyQualifiedCallerName() { return null; }
        @Override public boolean isTraceEnabled() { return false; }
        @Override public boolean isTraceEnabled(Marker m) { return false; }
        @Override public boolean isDebugEnabled() { return false; }
        @Override public boolean isDebugEnabled(Marker m) { return false; }
        @Override public boolean isInfoEnabled() { return false; }
        @Override public boolean isInfoEnabled(Marker m) { return false; }
        @Override public boolean isWarnEnabled() { return true; }
        @Override public boolean isWarnEnabled(Marker m) { return true; }
        @Override public boolean isErrorEnabled() { return true; }
        @Override public boolean isErrorEnabled(Marker m) { return true; }
    }

    private static class NonLoggingEventAwareLogger extends AbstractLogger {
        NonLoggingEventAwareLogger(String name) { this.name = name; }

        @Override
        protected void handleNormalizedLoggingCall(Level level, Marker marker,
                String msg, Object[] args, Throwable t) {
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
    }
}
