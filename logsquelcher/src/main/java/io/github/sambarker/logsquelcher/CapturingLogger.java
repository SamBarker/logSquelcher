package io.github.sambarker.logsquelcher;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.spi.LoggingEventAware;

import java.util.Optional;
import java.util.function.Predicate;

class CapturingLogger extends AbstractLogger implements LoggingEventAware {

    private final Optional<Logger> delegate;
    private volatile Level effectiveLevel;

    CapturingLogger(String name, Logger realLogger) {
        this.name = name;
        this.delegate = Optional.ofNullable(realLogger);
    }

    void setEffectiveLevel(Level level) {
        this.effectiveLevel = level;
    }

    void clearEffectiveLevel() {
        this.effectiveLevel = null;
    }

    Level getEffectiveLevel() {
        return this.effectiveLevel;
    }

    @Override
    public void log(LoggingEvent event) {
        var captured = new LogSquelcherLoggingEvent(
                event.getLevel(), name, event.getMessage(), event.getArgumentArray(),
                event.getThrowable(), event.getTimeStamp() > 0 ? event.getTimeStamp() : System.currentTimeMillis(),
                event.getThreadName() != null ? event.getThreadName() : Thread.currentThread().getName(),
                event.getKeyValuePairs());
        EventBuffer.capture(System.nanoTime(), captured);
        if (LogSquelcherConfig.REALTIME_LOGGING) {
            delegate.ifPresent(d -> CapturingLoggerFactory.forward(d, captured));
        }
    }

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker,
            String messagePattern, Object[] arguments, Throwable throwable) {
        var captured = new LogSquelcherLoggingEvent(
                level, name, messagePattern, arguments, throwable,
                System.currentTimeMillis(), Thread.currentThread().getName(), null);
        EventBuffer.capture(System.nanoTime(), captured);
        if (LogSquelcherConfig.REALTIME_LOGGING) {
            delegate.ifPresent(d -> CapturingLoggerFactory.forward(d, captured));
        }
    }

    @Override
    public String getFullyQualifiedCallerName() {
        return CapturingLogger.class.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return isLevelEnabled(Level.TRACE, Logger::isTraceEnabled);
    }

    private boolean isLevelEnabled(Level level, Predicate<Logger> delegateCheck) {
        Level effective = resolveEffectiveLevel();
        if (effective != null) {
            return level.toInt() >= effective.toInt();
        }
        return delegate.map(delegateCheck::test).orElse(true);
    }

    private Level resolveEffectiveLevel() {
        if (effectiveLevel != null) {
            return effectiveLevel;
        }
        // Check if ROOT logger has an effective level set
        if (!org.slf4j.Logger.ROOT_LOGGER_NAME.equals(name)) {
            Logger rootLogger = org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            if (rootLogger instanceof CapturingLogger root && root.effectiveLevel != null) {
                return root.effectiveLevel;
            }
        }
        return null;
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isLevelEnabled(Level.TRACE, l -> l.isTraceEnabled(marker));
    }

    @Override
    public boolean isDebugEnabled() {
        return isLevelEnabled(Level.DEBUG, Logger::isDebugEnabled);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isLevelEnabled(Level.DEBUG, l -> l.isDebugEnabled(marker));
    }

    @Override
    public boolean isInfoEnabled() {
        return isLevelEnabled(Level.INFO, Logger::isInfoEnabled);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isLevelEnabled(Level.INFO, l -> l.isInfoEnabled(marker));
    }

    @Override
    public boolean isWarnEnabled() {
        return isLevelEnabled(Level.WARN, Logger::isWarnEnabled);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isLevelEnabled(Level.WARN, l -> l.isWarnEnabled(marker));
    }

    @Override
    public boolean isErrorEnabled() {
        return isLevelEnabled(Level.ERROR, Logger::isErrorEnabled);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isLevelEnabled(Level.ERROR, l -> l.isErrorEnabled(marker));
    }
}
