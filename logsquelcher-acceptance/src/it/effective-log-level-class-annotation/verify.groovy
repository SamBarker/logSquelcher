def buildLog = new File(basedir, 'build.log').text

// The test asserts directly on CapturedLogs, so a clean run means the class-level
// @EffectiveLogLevel(DEBUG) was active in both @BeforeEach and the test body.
assert buildLog.contains('Tests run: 1, Failures: 0, Errors: 0, Skipped: 0') :
    "Expected the test to pass, proving class-level @EffectiveLogLevel applies to @BeforeEach " +
    "and the test body.\nBuild log:\n${buildLog}"
