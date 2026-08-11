package io.github.sambarker.logsquelcher.fixture;

import io.github.sambarker.logsquelcher.CapturedLogs;
import io.github.sambarker.logsquelcher.LogSquelcherExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Fixture for testing that CapturedLogs injection is guarded in concurrent execution mode.
 * Not named *Test so Surefire does not discover it directly.
 */
@ExtendWith(LogSquelcherExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class ConcurrentAssertingFixture {

    @Test
    public void injectsCapturedLogsInConcurrentMode(CapturedLogs logs) {
    }
}
