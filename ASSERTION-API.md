# logsquelcher — Assertion API

## Injecting `CapturedLogs`

Declare `CapturedLogs` as a test-method parameter — no annotation or field required.
The auto-registered `LogSquelcherExtension` (via `ServiceLoader`) resolves it:

```java
@Test
void shouldLogWarningWhenPluginMissing(CapturedLogs logs) {
    myService.doSomething();

    LoggingEventAssert.assertThat(logs.logged(MyService.class, Level.WARN))
        .singleElement()
        .formattedMessage()
        .isEqualTo("plugin not found");
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

// List overload — full AssertJ iterable assertions available
assertThat(logs.logged(MyService.class, Level.WARN)).singleElement()
    .formattedMessage().isEqualTo("plugin not found");
```

### String navigators

Both navigators return `AbstractStringAssert<?>`, giving access to the full AssertJ
string assertion API — `isEqualTo`, `contains`, `startsWith`, `matches`, etc.

| Method | What it navigates to |
|---|---|
| `messageTemplate()` | Raw SLF4J message template (`{}` placeholders unresolved) |
| `formattedMessage()` | SLF4J-formatted message with placeholders interpolated |

Use `messageTemplate()` when you want to assert on a fixed phrase without caring about
interpolated runtime values:

```java
// "caught exception: {} closing channel" — assert the fixed phrase, ignore the exception text
assertThat(logs.logged(MyService.class, Level.WARN).get(0))
    .messageTemplate()
    .contains("closing channel");
```

Use `formattedMessage()` for exact or partial assertions on the fully-resolved message:

```java
assertThat(logs.logged(MyService.class, Level.WARN).get(0))
    .formattedMessage()
    .isEqualTo("plugin foo not found");
```

### Key-value pairs (structured logging)

| Method | What it checks |
|---|---|
| `containsKeyValue(String key, Object value)` | Event key-value pairs contain the entry |
| `hasKeyValues(Map<String, ?>)` | Event key-value pairs contain all entries |

### Asserting "nothing was logged"

```java
assertThat(logs.logged(MyService.class, Level.WARN)).isEmpty();
```

## Controlling log levels with `@EffectiveLogLevel`

By default, `isXxxEnabled()` methods delegate to the backend logger's configured level.
If your backend defaults to INFO, code gated by `if (log.isDebugEnabled())` won't execute.

`@EffectiveLogLevel` overrides the backend's level:

```java
@Test
@EffectiveLogLevel(Level.DEBUG)
void testDebugLogging(CapturedLogs logs) {
    // isDebugEnabled() returns true regardless of backend config
    subject.doSomething();

    assertThat(logs.logged(MyService.class, Level.DEBUG))
        .isNotEmpty();
}
```

### Lifecycle semantics

- **Method-level `@EffectiveLogLevel`:** Applies to that test's entire execution —
  `@BeforeEach`, test method, and `@AfterEach`.
- **Class-level `@EffectiveLogLevel`:** Applies to the entire class lifecycle —
  `@BeforeAll`, all test executions, `@AfterAll`.
- **Method overrides class:** When both are present, the method-level annotation
  takes precedence for that test's execution.

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
