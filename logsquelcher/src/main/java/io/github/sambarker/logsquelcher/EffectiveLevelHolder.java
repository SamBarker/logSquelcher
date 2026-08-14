package io.github.sambarker.logsquelcher;

import org.slf4j.event.Level;

/**
 * Holds the current effective log level set via {@link EffectiveLogLevel} annotation.
 * <p>
 * This is global state updated by the extension's lifecycle hooks. Tests run sequentially
 * by default, so there's no contention. For {@code @Execution(CONCURRENT)} tests,
 * {@code CapturedLogs} injection is already blocked.
 */
class EffectiveLevelHolder {
    private static volatile Level effectiveLevel;

    static void set(Level level) {
        effectiveLevel = level;
    }

    static void clear() {
        effectiveLevel = null;
    }

    static Level get() {
        return effectiveLevel;
    }

    static boolean isEnabled(Level level) {
        Level effective = effectiveLevel;
        if (effective == null) {
            // No @EffectiveLogLevel annotation, delegate to backend via returning true
            // (CapturingLogger will still AND this with the backend's result)
            return true;
        }
        // When @EffectiveLogLevel is present, honor it regardless of backend config
        return level.toInt() >= effective.toInt();
    }
}
