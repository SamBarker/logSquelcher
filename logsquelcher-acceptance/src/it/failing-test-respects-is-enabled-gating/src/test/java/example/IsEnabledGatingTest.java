package example;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IsEnabledGatingTest {

    static final String WARN_MESSAGE = "warn from failing test - should appear in output";
    static final String GATED_WARN_MESSAGE = "isInfoEnabled returned true - should not appear";

    private static final Logger LOG = LoggerFactory.getLogger(IsEnabledGatingTest.class);

    @Test
    void failsAfterLogging() {
        LOG.warn(WARN_MESSAGE);
        if (LOG.isInfoEnabled()) {
            LOG.warn(GATED_WARN_MESSAGE);
        }
        throw new AssertionError("deliberate failure");
    }
}
