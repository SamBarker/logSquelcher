package example;

import io.github.sambarker.logsquelcher.EffectiveLogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

@EffectiveLogLevel(Level.INFO)
public class MethodOverridesClassLevelTest {

    static final String BEFORE_EACH_DEBUG_BLOCKED = "debug from beforeEach - should not appear";
    static final String BEFORE_EACH_INFO = "info from beforeEach - should appear";
    static final String TEST_DEBUG = "debug from test with method override - should appear";

    private static final Logger LOG = LoggerFactory.getLogger(MethodOverridesClassLevelTest.class);

    @BeforeEach
    void setup() {
        // For the test with method-level @EffectiveLogLevel(DEBUG), this runs at DEBUG
        // For tests without method-level annotation, this runs at class-level INFO
        if (LOG.isDebugEnabled()) {
            LOG.debug(BEFORE_EACH_DEBUG_BLOCKED);
        }
        LOG.info(BEFORE_EACH_INFO);
    }

    @Test
    @EffectiveLogLevel(Level.DEBUG)
    void methodLevelDebugOverridesClassLevelInfo() {
        // Method-level DEBUG should override class-level INFO
        // This means isDebugEnabled() returns true, and @BeforeEach ran at DEBUG too
        if (LOG.isDebugEnabled()) {
            LOG.debug(TEST_DEBUG);
        }

        throw new AssertionError("deliberate failure to trigger log replay");
    }
}
