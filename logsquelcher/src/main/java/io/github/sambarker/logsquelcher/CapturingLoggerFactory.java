package io.github.sambarker.logsquelcher;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LoggingEventAware;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class CapturingLoggerFactory implements ILoggerFactory {

    private final ILoggerFactory delegate;
    private final ConcurrentMap<String, CapturingLogger> loggers = new ConcurrentHashMap<>();

    CapturingLoggerFactory(ILoggerFactory delegate) {
        this.delegate = delegate;
    }

    ILoggerFactory getDelegate() {
        return delegate;
    }

    @Override
    public Logger getLogger(String name) {
        return loggers.computeIfAbsent(name, n -> {
            Logger real = delegate != null ? delegate.getLogger(n) : null;
            return new CapturingLogger(n, real);
        });
    }

    void replay(LoggingEvent event) {
        if (delegate == null) {
            return;
        }
        forward(delegate.getLogger(event.getLoggerName()), event);
    }

    static void forward(Logger real, LoggingEvent event) {
        if (!real.isEnabledForLevel(event.getLevel())) {
            return;
        }
        if (real instanceof LoggingEventAware lea) {
            lea.log(event);
        } else {
            // Fallback for non-LoggingEventAware backends (e.g. slf4j-simple): format
            // the message and call the level-specific method so the event reaches output.
            String formatted = MessageFormatter.arrayFormat(
                    event.getMessage(), event.getArgumentArray(), event.getThrowable()).getMessage();
            Throwable t = event.getThrowable();
            switch (event.getLevel()) {
                case ERROR -> real.error(formatted, t);
                case WARN  -> real.warn(formatted, t);
                case INFO  -> real.info(formatted, t);
                case DEBUG -> real.debug(formatted, t);
                case TRACE -> real.trace(formatted, t);
            }
        }
    }
}
