package example;

import io.github.sambarker.logsquelcher.EffectiveLogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

@EffectiveLogLevel(Level.DEBUG)
public class ClassLevelEffectiveLogLevelTest {

    static final String BEFORE_EACH_DEBUG = "debug from beforeEach with class-level annotation";
    static final String TEST_DEBUG = "debug from test with class-level annotation";

    private static final Logger LOG = LoggerFactory.getLogger(ClassLevelEffectiveLogLevelTest.class);

    @BeforeEach
    void setup() {
        // Class-level annotation should be active during @BeforeEach
        if (LOG.isDebugEnabled()) {
            LOG.debug(BEFORE_EACH_DEBUG);
        }
    }

    @Test
    void failsAfterLoggingDebugInBeforeEachAndTest() {
        // Class-level annotation should still be active in test method
        if (LOG.isDebugEnabled()) {
            LOG.debug(TEST_DEBUG);
        }

        throw new AssertionError("deliberate failure to trigger log replay");
    }
}
