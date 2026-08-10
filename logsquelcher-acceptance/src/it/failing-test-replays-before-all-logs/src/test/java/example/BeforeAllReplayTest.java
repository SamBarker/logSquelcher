package example;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeforeAllReplayTest {
    static final String BEFORE_ALL_MESSAGE = "log from beforeAll that should appear on failure";
    private static final Logger LOG = LoggerFactory.getLogger(BeforeAllReplayTest.class);

    @BeforeAll
    static void setUpClass() {
        LOG.warn(BEFORE_ALL_MESSAGE);
    }

    @Test
    void failsAfterBeforeAll() {
        throw new AssertionError("deliberate failure");
    }
}
