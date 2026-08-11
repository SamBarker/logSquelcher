def buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('live log from beforeAll in realtime mode') :
    "Expected @BeforeAll log message in build output but did not find it.\n" +
    "Build log:\n${buildLog}"
