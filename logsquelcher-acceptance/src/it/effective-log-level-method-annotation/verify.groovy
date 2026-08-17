def buildLog = new File(basedir, 'build.log').text

// The test asserts directly on CapturedLogs against an INFO backend, so a clean run means
// method-level @EffectiveLogLevel(DEBUG) enabled the otherwise-filtered debug logging.
assert buildLog.contains('Tests run: 1, Failures: 0, Errors: 0, Skipped: 0') :
    "Expected the test to pass, proving method-level @EffectiveLogLevel(DEBUG) enabled debug " +
    "logging the INFO backend would otherwise filter.\nBuild log:\n${buildLog}"
