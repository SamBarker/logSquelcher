def buildLog = new File(basedir, 'build.log').text

assert buildLog.contains('Tests run: 1, Failures: 0, Errors: 0, Skipped: 0') :
    "Expected the single test to run and pass, proving @EffectiveLogLevel self-registered the extension " +
    "without autodetection or explicit @ExtendWith.\nBuild log:\n${buildLog}"
