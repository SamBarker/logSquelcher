def buildLog = new File(basedir, 'build.log').text

assert buildLog.contains('warn from failing test - should appear in output') :
    "Expected WARN message to be replayed to output on failure but did not find it.\n" +
    "Build log:\n${buildLog}"

assert !buildLog.contains('info from failing test - should be suppressed by backend') :
    "Expected INFO message to be suppressed by the backend (root level WARN) but it appeared in output.\n" +
    "Build log:\n${buildLog}"
