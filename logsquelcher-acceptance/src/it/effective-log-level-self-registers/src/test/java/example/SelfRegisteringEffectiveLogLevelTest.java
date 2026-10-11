package example;

import io.github.sambarker.logsquelcher.EffectiveLogLevel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves {@code @EffectiveLogLevel} self-registers {@code LogSquelcherExtension} via its
 * meta-{@code @ExtendWith}.
 * <p>
 * This project deliberately has <strong>no</strong> {@code junit-platform.properties}
 * (so extension autodetection is OFF) and the class carries <strong>no</strong>
 * {@code @ExtendWith}. The only way the extension can be active is the meta-annotation on
 * {@code @EffectiveLogLevel} itself. If it is not, {@code isDebugEnabled()} delegates to the
 * logback backend (default INFO) and returns false, failing the test.
 */
public class SelfRegisteringEffectiveLogLevelTest {

    private static final Logger LOG = LoggerFactory.getLogger(SelfRegisteringEffectiveLogLevelTest.class);

    @Test
    @EffectiveLogLevel(logger = SelfRegisteringEffectiveLogLevelTest.class, level = Level.DEBUG)
    void isDebugEnabledIsTrueWithoutAutodetection() {
        assertTrue(LOG.isDebugEnabled(),
                "isDebugEnabled() should be true because @EffectiveLogLevel self-registers the extension");
    }
}
