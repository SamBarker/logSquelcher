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
 * Proves a class-level {@code @EffectiveLogLevel} applies to the whole class — {@code @BeforeEach}
 * and the test body alike — asserting directly on the captured logs instead of forcing a failure
 * and grepping the replayed output.
 * <p>
 * There is no {@code junit-platform.properties}: the class-level {@code @EffectiveLogLevel}
 * self-registers the extension via its meta-{@code @ExtendWith}.
 */
@EffectiveLogLevel(logger = ClassLevelEffectiveLogLevelTest.class, level = Level.DEBUG)
public class ClassLevelEffectiveLogLevelTest {

    static final String BEFORE_EACH_DEBUG = "debug from beforeEach with class-level annotation";
    static final String TEST_DEBUG = "debug from test with class-level annotation";

    private static final Logger LOG = LoggerFactory.getLogger(ClassLevelEffectiveLogLevelTest.class);

    @BeforeEach
    void setup() {
        // Class-level DEBUG should be active during @BeforeEach.
        if (LOG.isDebugEnabled()) {
            LOG.debug(BEFORE_EACH_DEBUG);
        }
    }

    @Test
    void classLevelAnnotationAppliesToBeforeEachAndTest(CapturedLogs logs) {
        // Class-level DEBUG should still be active in the test body.
        if (LOG.isDebugEnabled()) {
            LOG.debug(TEST_DEBUG);
        }

        assertThat(logs.logged(ClassLevelEffectiveLogLevelTest.class, Level.DEBUG))
                .anySatisfy(event -> assertThat(event).formattedMessage().isEqualTo(BEFORE_EACH_DEBUG))
                .anySatisfy(event -> assertThat(event).formattedMessage().isEqualTo(TEST_DEBUG));
    }
}
