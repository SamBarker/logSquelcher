def buildLog = new File(basedir, 'build.log').text

// The test fails on purpose. logsquelcher squelches logs during the run, so anything appearing in the
// build output can only have come from replay-on-failure.
assert buildLog.contains('warn-replayed-on-failure') :
    "Expected the WARN log to be replayed on failure, proving replay-on-failure surfaces the surviving " +
    "logs.\nBuild log:\n${buildLog}"

// @EffectiveLogLevel(WARN) made isInfoEnabled() false, so the guarded info was never logged or captured
// and therefore cannot be replayed. Absent the annotation the INFO backend would have logged it, so its
// absence proves the annotation raised the effective level.
assert !buildLog.contains('info-suppressed-by-effective-warn') :
    "The INFO log must not appear: @EffectiveLogLevel(WARN) should have suppressed it before capture.\n" +
    "Build log:\n${buildLog}"
