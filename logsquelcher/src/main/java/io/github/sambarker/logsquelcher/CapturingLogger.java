package io.github.sambarker.logsquelcher;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.spi.LoggingEventAware;

import java.util.Optional;

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
        Level effective = resolveEffectiveLevel();
        if (effective != null) {
            return Level.TRACE.toInt() >= effective.toInt();
        }
        return delegate.map(Logger::isTraceEnabled).orElse(true);
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
        Level effective = resolveEffectiveLevel();
        if (effective != null) {
            return Level.TRACE.toInt() >= effective.toInt();
        }
        return delegate.map(d -> d.isTraceEnabled(marker)).orElse(true);
    }

    @Override
    public boolean isDebugEnabled() {
        Level effective = resolveEffectiveLevel();
        if (effective != null) {
            return Level.DEBUG.toInt() >= effective.toInt();
        }
        return delegate.map(Logger::isDebugEnabled).orElse(true);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        Level effective = resolveEffectiveLevel();
        if (effective != null) {
            return Level.DEBUG.toInt() >= effective.toInt();
        }
        return delegate.map(d -> d.isDebugEnabled(marker)).orElse(true);
    }

    @Override
    public boolean isInfoEnabled() {
        Level effective = resolveEffectiveLevel();
        if (effective != null) {
            return Level.INFO.toInt() >= effective.toInt();
        }
        return delegate.map(Logger::isInfoEnabled).orElse(true);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        Level effective = resolveEffectiveLevel();
        if (effective != null) {
            return Level.INFO.toInt() >= effective.toInt();
        }
        return delegate.map(d -> d.isInfoEnabled(marker)).orElse(true);
    }

    @Override
    public boolean isWarnEnabled() {
        Level effective = resolveEffectiveLevel();
        if (effective != null) {
            return Level.WARN.toInt() >= effective.toInt();
        }
        return delegate.map(Logger::isWarnEnabled).orElse(true);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        Level effective = resolveEffectiveLevel();
        if (effective != null) {
            return Level.WARN.toInt() >= effective.toInt();
        }
        return delegate.map(d -> d.isWarnEnabled(marker)).orElse(true);
    }

    @Override
    public boolean isErrorEnabled() {
        Level effective = resolveEffectiveLevel();
        if (effective != null) {
            return Level.ERROR.toInt() >= effective.toInt();
        }
        return delegate.map(Logger::isErrorEnabled).orElse(true);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        Level effective = resolveEffectiveLevel();
        if (effective != null) {
            return Level.ERROR.toInt() >= effective.toInt();
        }
        return delegate.map(d -> d.isErrorEnabled(marker)).orElse(true);
    }
}
