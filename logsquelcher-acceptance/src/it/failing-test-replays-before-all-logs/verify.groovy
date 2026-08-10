def buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('log from beforeAll that should appear on failure') :
    "Expected @BeforeAll log message in build output on failure but did not find it.\n" +
    "Build log:\n${buildLog}"
