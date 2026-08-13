# logsquelcher

A JUnit 5 extension that silences log output during passing tests and replays it only on failure.

## The problem

Many test suites deliberately exercise failure modes — filters that reject malformed messages,
handlers that short-circuit on bad input, retry logic that expects transient errors. The code under
test legitimately logs warnings and stack traces as part of these scenarios.

The trouble is that this output appears during **every** test run, including passing ones. Hundreds
of stack traces scroll past on a green build, training developers to ignore the log output entirely
— which means real problems get lost in the noise.

A `ThreadLocal` approach doesn't help when the logging happens off the test thread. Frameworks like
[Kroxylicious](https://github.com/kroxylicious/kroxylicious) run filter code on Netty I/O threads
from a fixed shared pool, so thread-local capture misses exactly the events that matter.

## How it works

`logsquelcher` registers itself as the SLF4J provider and wraps the real logging backend (Logback,
Log4j2, or `slf4j-simple` as a last resort). All log events from all threads are captured into a
time-stamped global buffer.

A JUnit 5 extension records the start time of each test. On failure it extracts the events that
fell inside the test's window and replays them through the real backend — so they appear in the
normal console output. On success the events are silently discarded.

## Requirements

- Java 17+
- JUnit Jupiter 5.x
- SLF4J 2.x

## Installation

### As a test dependency

```xml
<dependency>
    <groupId>io.github.sambarker</groupId>
    <artifactId>logsquelcher</artifactId>
    <version>0.2.2</version>
    <scope>test</scope>
</dependency>
```

`slf4j-simple` ships as a transitive dependency and acts as a fallback backend when no other SLF4J
backend is on the classpath. If you already have Logback or Log4j2, they are preferred automatically.

### As a Surefire dependency (recommended for multi-module projects)

Adding logsquelcher to Surefire's classpath rather than each module's test dependencies keeps the
extension out of individual module POMs. Only modules that use the assertion API (`ext.logged(...)`,
`ext.assertNotLogged(...)`) need a direct `<scope>test</scope>` dependency.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <dependencies>
        <dependency>
            <groupId>io.github.sambarker</groupId>
            <artifactId>logsquelcher</artifactId>
            <version>0.2.2</version>
        </dependency>
    </dependencies>
    <configuration>
        <systemPropertyVariables>
            <slf4j.provider>io.github.sambarker.logsquelcher.LogSquelcherSLF4JProvider</slf4j.provider>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

Pinning the SLF4J provider via `systemPropertyVariables` ensures logsquelcher wins when multiple
providers are on the classpath.

## Registration

### Automatic (recommended)

Add to `src/test/resources/junit-platform.properties`:

```properties
junit.jupiter.extensions.autodetection.enabled=true
```

The extension and SLF4J provider register themselves via `ServiceLoader` — no per-class annotation
needed.

### Per-class

```java
@ExtendWith(LogSquelcherExtension.class)
class MyTest { ... }
```

## Usage

With the extension registered, tests require no changes. Logs are silenced on green runs and
replayed automatically on failure.

### Asserting log output in tests

Inject `CapturedLogs` as a test-method parameter to query captured events:

```java
@Test
void warningIsLoggedWhenPluginIsDeprecated(CapturedLogs logs) {
    subject.doSomething();

    LoggingEventAssert.assertThat(logs.logged(MyService.class, Level.WARN))
            .singleElement()
            .formattedMessage()
            .isEqualTo("Plugin is deprecated");
}
```

`logs.logged(Class, Level)` returns a `List<LoggingEvent>` — use AssertJ's iterable
assertions to check size, then navigate into individual events with `LoggingEventAssert`.

Use `logs.logged(Class)` to match any level.

### Partial-match on message template

To assert that a specific fixed phrase was logged without caring about interpolated values,
navigate to the raw SLF4J template:

```java
LoggingEventAssert.assertThat(logs.logged(MyService.class, Level.WARN).get(0))
        .messageTemplate()
        .contains("closing channel");
```

### Key-value pairs (structured logging)

```java
var event = LoggingEventAssert.assertThat(logs.logged(MyService.class, Level.WARN).get(0));
event.formattedMessage().isEqualTo("Plugin is deprecated");
event.containsKeyValue("filterName", "myFilterDef");
```

### Negative assertion

```java
assertThat(logs.logged(MyService.class, Level.ERROR)).isEmpty();
```

## Realtime mode

By default logsquelcher buffers all events and only replays them when a test fails. If you want
to see logs stream immediately — for example while debugging a test interactively — enable
realtime mode:

```
-Dlogsquelcher.realtimelogging=true
```

In realtime mode all events are forwarded to the backend as they arrive. Replay on failure is
skipped because the events were already written. Realtime mode is automatically enabled for
test classes annotated `@Execution(ExecutionMode.CONCURRENT)` because the time-window approach
cannot reliably isolate one test's events from another's in that context.

## IntelliJ IDEA

IntelliJ's JUnit runner uses the JUnit Platform Launcher, which reads `junit-platform.properties`
from the test classpath the same way Maven does. If you have followed the automatic registration
steps above — adding `junit.jupiter.extensions.autodetection.enabled=true` to
`src/test/resources/junit-platform.properties` — the extension will be loaded in IntelliJ without
any additional configuration.

If you cannot add that file (for example in a project you do not own), annotate each test class
directly:

```java
@ExtendWith(LogSquelcherExtension.class)
class MyTest { ... }
```

### Pinning the SLF4J provider in IntelliJ

If multiple SLF4J providers are on the classpath and IntelliJ picks the wrong one, pin it via
the default JUnit run configuration template VM options:

```
-Dslf4j.provider=io.github.sambarker.logsquelcher.LogSquelcherSLF4JProvider
```

If your project already sets this in Surefire's `systemPropertyVariables`, IntelliJ's Maven
integration may sync it automatically — check the SLF4J startup output to confirm which provider
was loaded before adding it manually.

### Realtime mode in IntelliJ

Add the VM option to the default JUnit run configuration template so that every test run picks
it up automatically:

```
-Dlogsquelcher.realtimelogging=true
```

The resulting template can be committed under `.idea/runConfigurations/` if you want to share
it with the team.

## Backend compatibility

`logsquelcher` wraps whichever SLF4J provider it finds. Preference order:

1. Any real backend (Logback, Log4j2, etc.)
2. `slf4j-simple` (bundled as fallback)

When multiple providers are on the classpath SLF4J will warn about the ambiguity and pick one.
In most Maven setups `logsquelcher` wins because it is a direct dependency. If your project declares
Logback as a direct dependency and it takes precedence, pin the provider explicitly in Surefire:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <systemPropertyVariables>
            <slf4j.provider>io.github.sambarker.logsquelcher.LogSquelcherSLF4JProvider</slf4j.provider>
        </systemPropertyVariables>
    </configuration>
</plugin>
```
