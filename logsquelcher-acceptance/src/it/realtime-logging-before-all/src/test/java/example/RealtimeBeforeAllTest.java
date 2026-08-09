package example;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealtimeBeforeAllTest {
    static final String BEFORE_ALL_MESSAGE = "live log from beforeAll in realtime mode";
    private static final Logger LOG = LoggerFactory.getLogger(RealtimeBeforeAllTest.class);

    @BeforeAll
    static void setUpClass() {
        LOG.warn(BEFORE_ALL_MESSAGE);
    }

    @Test
    void logsAndPasses() {
    }
}
