package io.github.sambarker.logsquelcher;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.LoggerFactory;
import org.slf4j.event.LoggingEvent;

import java.util.List;

public class LogSquelcherExtension implements BeforeAllCallback, BeforeEachCallback, TestWatcher, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(LogSquelcherExtension.class);
    private static final String CAPTURED_LOGS_KEY = "capturedLogs";
    private static final String CLASS_START_NANOS_KEY = "classStartNanos";
    private static final String BEFORE_ALL_SNAPSHOT_KEY = "beforeAllSnapshot";

    @Override
    public void beforeAll(ExtensionContext context) {
        store(context).put(CLASS_START_NANOS_KEY, System.nanoTime());
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        long testStartNanos = System.nanoTime();
        ExtensionContext.Store classStore = store(context.getParent().orElseThrow());
        if (classStore.get(BEFORE_ALL_SNAPSHOT_KEY) == null) {
            long classStartNanos = classStore.getOrDefault(CLASS_START_NANOS_KEY, Long.class, testStartNanos);
            classStore.put(BEFORE_ALL_SNAPSHOT_KEY, EventBuffer.extractWindow(classStartNanos, testStartNanos));
        }
        store(context).put(CAPTURED_LOGS_KEY, new CapturedLogs(testStartNanos));
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        if (LogSquelcherConfig.REALTIME_LOGGING) {
            return;
        }
        ExtensionContext.Store classStore = store(context.getParent().orElseThrow());

        @SuppressWarnings("unchecked")
        List<CapturedEvent> beforeAllSnapshot = (List<CapturedEvent>) classStore.get(BEFORE_ALL_SNAPSHOT_KEY);
        if (beforeAllSnapshot != null) {
            beforeAllSnapshot.forEach(e -> replay(e.loggingEvent()));
        }

        CapturedLogs logs = store(context).get(CAPTURED_LOGS_KEY, CapturedLogs.class);
        if (logs != null) {
            EventBuffer.extractWindow(logs.startNanos(), System.nanoTime())
                    .forEach(e -> replay(e.loggingEvent()));
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == CapturedLogs.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return store(extensionContext).get(CAPTURED_LOGS_KEY, CapturedLogs.class);
    }

    private static void replay(LoggingEvent event) {
        if (LoggerFactory.getILoggerFactory() instanceof CapturingLoggerFactory factory) {
            factory.replay(event);
        }
    }

    private static ExtensionContext.Store store(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }
}
