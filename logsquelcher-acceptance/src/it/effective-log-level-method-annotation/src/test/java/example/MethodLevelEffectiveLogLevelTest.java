package example;

import io.github.sambarker.logsquelcher.EffectiveLogLevel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class MethodLevelEffectiveLogLevelTest {

    static final String DEBUG_MESSAGE = "debug message that should appear with annotation";
    static final String GATED_DEBUG_MESSAGE = "isDebugEnabled returned true - annotation working";

    private static final Logger LOG = LoggerFactory.getLogger(MethodLevelEffectiveLogLevelTest.class);

    @Test
    @EffectiveLogLevel(Level.DEBUG)
    void failsAfterLoggingDebug() {
        // With @EffectiveLogLevel(DEBUG), isDebugEnabled() should return true
        // even though the backend (logback with no config) defaults to a higher level
        LOG.debug(DEBUG_MESSAGE);

        if (LOG.isDebugEnabled()) {
            LOG.debug(GATED_DEBUG_MESSAGE);
        }

        throw new AssertionError("deliberate failure to trigger log replay");
    }
}
