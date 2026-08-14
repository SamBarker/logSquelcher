def buildLog = new File(basedir, 'build.log').text

assert buildLog.contains('debug from beforeEach with class-level annotation') :
    "Expected DEBUG message from @BeforeEach when class has @EffectiveLogLevel(DEBUG) but did not find it.\n" +
    "Build log:\n${buildLog}"

assert buildLog.contains('debug from test with class-level annotation') :
    "Expected DEBUG message from test method when class has @EffectiveLogLevel(DEBUG) but did not find it.\n" +
    "Build log:\n${buildLog}"
