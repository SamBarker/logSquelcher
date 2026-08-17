def buildLog = new File(basedir, 'build.log').text

// The tests assert directly on CapturedLogs, so a clean run means method-level
// @EffectiveLogLevel overrode the class-level one (present) and, without an override,
// the class INFO level suppressed DEBUG (absent).
assert buildLog.contains('Tests run: 2, Failures: 0, Errors: 0, Skipped: 0') :
    "Expected both tests to pass, proving method-level @EffectiveLogLevel overrides class-level " +
    "and that CapturedLogs reflects the effective level.\nBuild log:\n${buildLog}"
