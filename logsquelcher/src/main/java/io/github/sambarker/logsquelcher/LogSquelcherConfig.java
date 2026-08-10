package io.github.sambarker.logsquelcher;

class LogSquelcherConfig {
    static boolean REALTIME_LOGGING = Boolean.getBoolean("logsquelcher.realtimelogging");
    static boolean ENABLE_ASSERTIONS_ON_INTERLEAVED_LOGS = Boolean.getBoolean("logsquelcher.enableAssertionsOnInterleavedLogs");
}
