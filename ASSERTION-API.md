# logsquelcher — Assertion API

## Injecting `CapturedLogs`

Declare `CapturedLogs` as a test-method parameter — no annotation or field required.
The auto-registered `LogSquelcherExtension` (via `ServiceLoader`) resolves it:

```java
@Test
void shouldLogWarningWhenPluginMissing(CapturedLogs logs) {
    myService.doSomething();
    LoggingEventAssert.assertThat(logs.logged(MyService.class, Level.WARN))
        .hasSize(1)
        .first()
        .satisfies(e -> LoggingEventAssert.assertThat(e)
            .hasFormattedMessage("plugin not found"));
}
```

`CapturedLogs` is scoped to the current test's time window and covers all threads.

## `CapturedLogs` query API

```java
// All events from logger at level since the test started
List<LoggingEvent> logged(Class<?> logger, Level level)

// All events from logger at any level since the test started
List<LoggingEvent> logged(Class<?> logger)

// All events regardless of logger or level since the test started
List<LoggingEvent> logged()
```

All overloads return an empty list — never null — if nothing was captured.

## `LoggingEventAssert`

`LoggingEventAssert` is an AssertJ-style assertion for individual `LoggingEvent` instances.
`LoggingEventAssert.assertThat(List<LoggingEvent>)` returns `LoggingEventsAssert`, which
extends `AbstractIterableAssert` and gives access to the full AssertJ iterable API.

```java
import static io.github.sambarker.logsquelcher.LoggingEventAssert.assertThat;

// Single event
assertThat(logs.logged(MyService.class, Level.WARN).get(0))
    .hasFormattedMessage("plugin not found")
    .containsKeyValue("filterName", "myFilterDef");

// List overload — full AssertJ iterable assertions available
assertThat(logs.logged(MyService.class, Level.WARN))
    .hasSize(1);
```

### Available assertions

| Method | What it checks |
|---|---|
| `hasFormattedMessage(String)` | SLF4J-formatted message equals (exact match) |
| `containsKeyValue(String key, Object value)` | Event key-value pairs contain the entry |
| `hasKeyValues(Map<String, ?>)` | Event key-value pairs contain all entries |

### Asserting "nothing was logged"

Use the `logged(...)` return value directly with AssertJ:

```java
assertThat(logs.logged(MyService.class, Level.WARN)).isEmpty();
```

### Known gap — non-exact message matching

`hasFormattedMessage` only supports exact equality. There is currently no
`formattedMessageContains` or `formattedMessageMatches` helper. As a workaround,
use AssertJ's `anyMatch` on the list:

```java
assertThat(logs.logged(MyService.class, Level.WARN))
    .anyMatch(e -> {
        String msg = MessageFormatter.arrayFormat(e.getMessage(), e.getArgumentArray(), null).getMessage();
        return msg.contains("plugin not found");
    });
```

## Concurrent execution

`CapturedLogs` injection is blocked in `@Execution(CONCURRENT)` test classes by default,
because log events from concurrent tests interleave and cannot be reliably attributed
to a single test. Set `-Dlogsquelcher.enableAssertionsOnInterleavedLogs=true` to opt in
(you accept that assertions may see each other's events).

## Replacing logcaptor

| logcaptor | logsquelcher equivalent |
|---|---|
| `LogCaptor.forClass(Foo.class)` | `CapturedLogs logs` parameter |
| `logCaptor.getLogEvents()` | `logs.logged(Foo.class)` |
| `logCaptor.getWarnLogs()` | `logs.logged(Foo.class, Level.WARN)` |
| `assertThat(logCaptor.getWarnLogs()).isEmpty()` | `assertThat(logs.logged(Foo.class, Level.WARN)).isEmpty()` |
