package io.github.sambarker.logsquelcher.fixture;

import io.github.sambarker.logsquelcher.LogSquelcherExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fixture for testing @BeforeAll log replay. Not named *Test so Surefire does not discover it directly.
 */
@ExtendWith(LogSquelcherExtension.class)
public class BeforeAllLoggingFixture {

    public static final String BEFORE_ALL_MESSAGE = "before all message that should appear on failure";

    private static final Logger LOG = LoggerFactory.getLogger(BeforeAllLoggingFixture.class);

    @BeforeAll
    static void setUp() {
        LOG.warn(BEFORE_ALL_MESSAGE);
    }

    @Test
    public void failingTestAfterBeforeAll() {
        throw new AssertionError("deliberate failure");
    }
}
