def buildLog = new File(basedir, 'build.log').text

assert buildLog.contains('debug message that should appear with annotation') :
    "Expected DEBUG message to be replayed when @EffectiveLogLevel(DEBUG) is present but did not find it.\n" +
    "Build log:\n${buildLog}"

assert buildLog.contains('isDebugEnabled returned true - annotation working') :
    "Expected isDebugEnabled() to return true when @EffectiveLogLevel(DEBUG) is present, but the guarded block was not entered.\n" +
    "Build log:\n${buildLog}"
