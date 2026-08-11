package example;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendLevelTest {

    static final String WARN_MESSAGE = "warn from failing test - should appear in output";
    static final String INFO_MESSAGE = "info from failing test - should be suppressed by backend";

    private static final Logger LOG = LoggerFactory.getLogger(BackendLevelTest.class);

    @Test
    void failsAfterLogging() {
        LOG.warn(WARN_MESSAGE);
        LOG.info(INFO_MESSAGE);
        throw new AssertionError("deliberate failure");
    }
}
