package io.github.sambarker.logsquelcher.fixture;

import io.github.sambarker.logsquelcher.LogSquelcherExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fixture for testing that replay is suppressed in concurrent execution mode.
 * Not named *Test so Surefire does not discover it directly.
 */
@ExtendWith(LogSquelcherExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class ConcurrentFailingFixture {

    public static final String SUPPRESSED_MESSAGE = "this message should not be replayed in concurrent mode";

    private static final Logger LOG = LoggerFactory.getLogger(ConcurrentFailingFixture.class);

    @Test
    public void failingTestInConcurrentMode() {
        LOG.warn(SUPPRESSED_MESSAGE);
        throw new AssertionError("deliberate failure");
    }
}
