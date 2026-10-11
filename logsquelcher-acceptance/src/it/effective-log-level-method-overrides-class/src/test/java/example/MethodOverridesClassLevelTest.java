package example;

import io.github.sambarker.logsquelcher.CapturedLogs;
import io.github.sambarker.logsquelcher.EffectiveLogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static io.github.sambarker.logsquelcher.LoggingEventAssert.assertThat;

/**
 * Proves method-level {@code @EffectiveLogLevel} overrides a class-level one, asserting directly
 * on the captured logs rather than forcing a failure and grepping the replayed output.
 * <p>
 * The class is pinned to INFO. One test overrides to DEBUG (so its guarded debug logs — in both
 * {@code @BeforeEach} and the test body — are emitted and captured); a sibling test with no
 * override runs at the class INFO level (so the guarded debug is suppressed and absent from the
 * capture). Asserting <em>absence</em> is something the old fail-and-grep approach could never do.
 * <p>
 * There is no {@code junit-platform.properties} here: {@code @EffectiveLogLevel} self-registers the
 * extension (via its meta-{@code @ExtendWith}), and the class-level annotation registers it for the
 * whole class, including the non-overriding test that only injects {@link CapturedLogs}.
 */
@EffectiveLogLevel(logger = MethodOverridesClassLevelTest.class, level = Level.INFO)
public class MethodOverridesClassLevelTest {

    static final String BEFORE_EACH_DEBUG = "debug from beforeEach";
    static final String BEFORE_EACH_INFO = "info from beforeEach";
    static final String TEST_DEBUG = "debug from test with method override";

    private static final Logger LOG = LoggerFactory.getLogger(MethodOverridesClassLevelTest.class);

    @BeforeEach
    void setup() {
        // Runs at method-level DEBUG for the overriding test, class-level INFO otherwise.
        if (LOG.isDebugEnabled()) {
            LOG.debug(BEFORE_EACH_DEBUG);
        }
        LOG.info(BEFORE_EACH_INFO);
    }

    @Test
    @EffectiveLogLevel(logger = MethodOverridesClassLevelTest.class, level = Level.DEBUG)
    void methodLevelDebugOverridesClassLevelInfo(CapturedLogs logs) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(TEST_DEBUG);
        }

        // Method-level DEBUG overrode class-level INFO, so DEBUG was enabled during @BeforeEach and
        // the test body: both guarded debug messages were emitted and captured.
        assertThat(logs.logged(MethodOverridesClassLevelTest.class, Level.DEBUG))
                .anySatisfy(event -> assertThat(event).formattedMessage().isEqualTo(BEFORE_EACH_DEBUG))
                .anySatisfy(event -> assertThat(event).formattedMessage().isEqualTo(TEST_DEBUG));
    }

    @Test
    void classLevelInfoAppliesWithoutOverride(CapturedLogs logs) {
        // No method-level override: the class INFO level is in effect, so isDebugEnabled() is false
        // and the guarded debug in @BeforeEach was never emitted.
        assertThat(logs.logged(MethodOverridesClassLevelTest.class, Level.DEBUG)).isEmpty();
        assertThat(logs.logged(MethodOverridesClassLevelTest.class, Level.INFO))
                .anySatisfy(event -> assertThat(event).formattedMessage().isEqualTo(BEFORE_EACH_INFO));
    }
}
