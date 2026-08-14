package io.github.sambarker.logsquelcher;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.LoggingEvent;

import java.util.ArrayList;
import java.util.List;

public class LogSquelcherExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback,
        AfterEachCallback, TestWatcher, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(LogSquelcherExtension.class);
    private static final String CAPTURED_LOGS_KEY = "capturedLogs";
    private static final String CLASS_START_NANOS_KEY = "classStartNanos";
    private static final String BEFORE_ALL_SNAPSHOT_KEY = "beforeAllSnapshot";
    private static final String EFFECTIVE_REALTIME_KEY = "effectiveRealtime";
    private static final String MODIFIED_LOGGERS_KEY = "modifiedLoggers";

    @Override
    public void beforeAll(ExtensionContext context) {
        ExtensionContext.Store classStore = store(context);
        classStore.put(CLASS_START_NANOS_KEY, System.nanoTime());
        boolean effectiveRealtime = LogSquelcherConfig.REALTIME_LOGGING
                || context.getExecutionMode() == ExecutionMode.CONCURRENT;
        classStore.put(EFFECTIVE_REALTIME_KEY, effectiveRealtime);

        // Class-level @EffectiveLogLevel applies to everything in this class
        List<CapturingLogger> modified = applyEffectiveLevels(context);
        if (!modified.isEmpty()) {
            classStore.put(MODIFIED_LOGGERS_KEY, modified);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // Clear class-level effective log levels
        clearModifiedLoggers(store(context));
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

        // Method-level @EffectiveLogLevel overrides class-level for this test
        List<CapturingLogger> modified = applyEffectiveLevels(context);
        if (!modified.isEmpty()) {
            store(context).put(MODIFIED_LOGGERS_KEY, modified);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        // Clear method-level effective log levels and restore class-level if present
        clearModifiedLoggers(store(context));

        // Restore class-level settings if they exist
        ExtensionContext.Store classStore = store(context.getParent().orElseThrow());
        @SuppressWarnings("unchecked")
        List<CapturingLogger> classLoggers = (List<CapturingLogger>) classStore.get(MODIFIED_LOGGERS_KEY);
        if (classLoggers != null) {
            // Re-apply class-level annotations that were overridden
            applyEffectiveLevels(context.getParent().orElseThrow());
        }
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        ExtensionContext.Store classStore = store(context.getParent().orElseThrow());
        boolean effectiveRealtime = classStore.getOrDefault(EFFECTIVE_REALTIME_KEY, Boolean.class, false);
        if (effectiveRealtime) {
            return;
        }

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
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
        boolean concurrent = context.getParent()
                .map(c -> c.getExecutionMode() == ExecutionMode.CONCURRENT)
                .orElse(false);
        if (concurrent && !LogSquelcherConfig.ENABLE_ASSERTIONS_ON_INTERLEAVED_LOGS) {
            throw new ExtensionConfigurationException(
                    "CapturedLogs cannot isolate logs from a specific test in concurrent execution mode. " +
                    "Ensure your assertions can. Set -Dlogsquelcher.enableAssertionsOnInterleavedLogs=true to opt in.");
        }
        return store(context).get(CAPTURED_LOGS_KEY, CapturedLogs.class);
    }

    private static void replay(LoggingEvent event) {
        if (LoggerFactory.getILoggerFactory() instanceof CapturingLoggerFactory factory) {
            factory.replay(event);
        }
    }

    private static ExtensionContext.Store store(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }

    private static List<CapturingLogger> applyEffectiveLevels(ExtensionContext context) {
        List<CapturingLogger> modified = new ArrayList<>();

        // Find all @EffectiveLogLevel annotations (handles both single and @EffectiveLevels container)
        EffectiveLogLevel[] annotations = context.getElement()
                .map(element -> element.getAnnotationsByType(EffectiveLogLevel.class))
                .orElse(new EffectiveLogLevel[0]);

        for (EffectiveLogLevel annotation : annotations) {
            Logger logger = LoggerFactory.getLogger(annotation.logger());
            if (logger instanceof CapturingLogger capturing) {
                capturing.setEffectiveLevel(annotation.level());
                modified.add(capturing);
            }
        }

        return modified;
    }

    private static void clearModifiedLoggers(ExtensionContext.Store contextStore) {
        @SuppressWarnings("unchecked")
        List<CapturingLogger> modified = (List<CapturingLogger>) contextStore.get(MODIFIED_LOGGERS_KEY);
        if (modified != null) {
            modified.forEach(CapturingLogger::clearEffectiveLevel);
        }
    }
}
