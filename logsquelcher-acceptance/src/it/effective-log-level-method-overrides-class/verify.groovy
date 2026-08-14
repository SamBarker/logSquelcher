def buildLog = new File(basedir, 'build.log').text

assert buildLog.contains('debug from beforeEach - should not appear') :
    "Expected DEBUG from @BeforeEach when method has @EffectiveLogLevel(DEBUG) overriding class INFO.\n" +
    "Build log:\n${buildLog}"

assert buildLog.contains('info from beforeEach - should appear') :
    "Expected INFO message from @BeforeEach but did not find it.\n" +
    "Build log:\n${buildLog}"

assert buildLog.contains('debug from test with method override - should appear') :
    "Expected DEBUG message from test method when method has @EffectiveLogLevel(DEBUG) but did not find it.\n" +
    "Build log:\n${buildLog}"
