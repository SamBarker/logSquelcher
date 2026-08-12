def buildLog = new File(basedir, 'build.log').text

assert buildLog.contains('warn from failing test - should appear in output') :
    "Expected WARN message to be replayed to output on failure but did not find it.\n" +
    "Build log:\n${buildLog}"

assert !buildLog.contains('isInfoEnabled returned true - should not appear') :
    "Expected isInfoEnabled() to return false for a WARN-level backend, but the guarded block was entered.\n" +
    "Build log:\n${buildLog}"
