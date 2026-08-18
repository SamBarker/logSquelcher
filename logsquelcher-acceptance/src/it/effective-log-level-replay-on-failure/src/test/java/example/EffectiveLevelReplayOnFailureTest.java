package example;

import io.github.sambarker.logsquelcher.EffectiveLogLevel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Demonstrates logsquelcher's replay-on-failure alongside {@code @EffectiveLogLevel} used to <em>raise</em>
 * the bar rather than lower it.
 * <p>
 * The backend is pinned to INFO. {@code @EffectiveLogLevel(WARN)} makes {@code isInfoEnabled()} return
 * false, so the guarded info log is never emitted (and so never captured); the guarded warn log is
 * emitted and captured. Because logsquelcher squelches logs during a green run, the warn only reaches
 * the build output when the test fails and its captured logs are replayed.
 * <p>
 * This deliberately goes "the other way": we never ask the backend to replay something below its own
 * threshold, so no custom rendering or backend manipulation is needed. The replayed warn (WARN >= the
 * INFO backend) passes straight through, while the info is gone before capture ever sees it.
 * <p>
 * There is no {@code junit-platform.properties}: the method-level {@code @EffectiveLogLevel}
 * self-registers the extension via its meta-{@code @ExtendWith}.
 */
public class EffectiveLevelReplayOnFailureTest {

    static final String SUPPRESSED_INFO = "info-suppressed-by-effective-warn";
    static final String REPLAYED_WARN = "warn-replayed-on-failure";

    private static final Logger LOG = LoggerFactory.getLogger(EffectiveLevelReplayOnFailureTest.class);

    @Test
    @EffectiveLogLevel(logger = EffectiveLevelReplayOnFailureTest.class, level = Level.WARN)
    void onlyWarnAndAboveSurviveAndAreReplayed() {
        // Guarded: @EffectiveLogLevel(WARN) makes isInfoEnabled() false, so this never logs.
        if (LOG.isInfoEnabled()) {
            LOG.info(SUPPRESSED_INFO);
        }
        // Guarded: WARN is still enabled, so this is captured and later replayed on failure.
        if (LOG.isWarnEnabled()) {
            LOG.warn(REPLAYED_WARN);
        }

        fail("forcing failure so logsquelcher replays the surviving logs");
    }
}
