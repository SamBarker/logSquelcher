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
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;

import java.util.List;
import java.util.Optional;

public class LogSquelcherExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback,
        AfterEachCallback, TestWatcher, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(LogSquelcherExtension.class);
    private static final String CAPTURED_LOGS_KEY = "capturedLogs";
    private static final String CLASS_START_NANOS_KEY = "classStartNanos";
    private static final String BEFORE_ALL_SNAPSHOT_KEY = "beforeAllSnapshot";
    private static final String EFFECTIVE_REALTIME_KEY = "effectiveRealtime";
    private static final String CLASS_LEVEL_KEY = "classLevel";

    @Override
    public void beforeAll(ExtensionContext context) {
        ExtensionContext.Store classStore = store(context);
        classStore.put(CLASS_START_NANOS_KEY, System.nanoTime());
        boolean effectiveRealtime = LogSquelcherConfig.REALTIME_LOGGING
                || context.getExecutionMode() == ExecutionMode.CONCURRENT;
        classStore.put(EFFECTIVE_REALTIME_KEY, effectiveRealtime);

        // Class-level @EffectiveLogLevel applies to everything in this class
        findEffectiveLogLevel(context).ifPresent(level -> {
            classStore.put(CLASS_LEVEL_KEY, level);
            EffectiveLevelHolder.set(level);
        });
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // Clear class-level effective log level
        if (store(context).get(CLASS_LEVEL_KEY) != null) {
            EffectiveLevelHolder.clear();
        }
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

        // Set up effective log level for this test execution:
        // 1. Method-level annotation overrides class-level
        // 2. Class-level annotation (if no method-level)
        // 3. No annotation (delegate to backend)
        Optional<Level> methodLevel = findEffectiveLogLevel(context);
        if (methodLevel.isPresent()) {
            // Method-level annotation applies to @BeforeEach + test + @AfterEach
            EffectiveLevelHolder.set(methodLevel.get());
        } else {
            // Check for class-level annotation
            if (classStore.get(CLASS_LEVEL_KEY) == null) {
                // Nested class without @BeforeAll - find and cache class-level annotation
                findEffectiveLogLevel(context.getParent().orElseThrow()).ifPresent(level -> {
                    classStore.put(CLASS_LEVEL_KEY, level);
                    EffectiveLevelHolder.set(level);
                });
            } else {
                // Restore class-level annotation if it was cleared
                Level classLevel = classStore.get(CLASS_LEVEL_KEY, Level.class);
                if (classLevel != null && EffectiveLevelHolder.get() == null) {
                    EffectiveLevelHolder.set(classLevel);
                }
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        // If this test had a method-level annotation, restore class-level (or clear)
        Optional<Level> methodLevel = findEffectiveLogLevel(context);
        if (methodLevel.isPresent()) {
            ExtensionContext.Store classStore = store(context.getParent().orElseThrow());
            Level classLevel = classStore.get(CLASS_LEVEL_KEY, Level.class);
            if (classLevel != null) {
                EffectiveLevelHolder.set(classLevel);
            } else {
                EffectiveLevelHolder.clear();
            }
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

    private static Optional<Level> findEffectiveLogLevel(ExtensionContext context) {
        return context.getElement()
                .flatMap(element -> Optional.ofNullable(element.getAnnotation(EffectiveLogLevel.class)))
                .map(EffectiveLogLevel::value);
    }
}
