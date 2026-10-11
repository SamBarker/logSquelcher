package example;

import io.github.sambarker.logsquelcher.CapturedLogs;
import io.github.sambarker.logsquelcher.EffectiveLogLevel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static io.github.sambarker.logsquelcher.LoggingEventAssert.assertThat;

/**
 * Proves a method-level {@code @EffectiveLogLevel} enables logging that the backend would otherwise
 * filter, asserting directly on the captured logs rather than forcing a failure and grepping the
 * replayed output.
 * <p>
 * The backend (logback) is pinned to INFO, so absent the annotation {@code isDebugEnabled()} would
 * be false and the guarded debug would never be emitted. Its presence in the capture is what proves
 * {@code @EffectiveLogLevel(DEBUG)} took effect.
 * <p>
 * There is no {@code junit-platform.properties}: the method-level {@code @EffectiveLogLevel}
 * self-registers the extension via its meta-{@code @ExtendWith}.
 */
public class MethodLevelEffectiveLogLevelTest {

    static final String GATED_DEBUG_MESSAGE = "debug gated by isDebugEnabled - annotation working";

    private static final Logger LOG = LoggerFactory.getLogger(MethodLevelEffectiveLogLevelTest.class);

    @Test
    @EffectiveLogLevel(logger = MethodLevelEffectiveLogLevelTest.class, level = Level.DEBUG)
    void methodLevelAnnotationEnablesDebug(CapturedLogs logs) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(GATED_DEBUG_MESSAGE);
        }

        assertThat(logs.logged(MethodLevelEffectiveLogLevelTest.class, Level.DEBUG))
                .singleElement()
                .formattedMessage()
                .isEqualTo(GATED_DEBUG_MESSAGE);
    }
}
