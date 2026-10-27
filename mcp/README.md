# JMC MCP Server

An MCP ([Model Context Protocol](https://modelcontextprotocol.io/)) server that exposes the JDK Mission Control flight
recording analysis capabilities as tools for LLM clients like Claude.

It is the headless sibling of the JMC AI view: the same kind of analysis, but driven from any MCP client instead of from
inside the JMC UI. Where the AI view reads the recording open in the active editor, this server loads `.jfr` files from
disk, so it works in a terminal, in CI, or anywhere else an agent runs.

Tools operate on JMC's `IItemCollection` model, using JMC filters for selection and JMC aggregators for statistics.
Event counts are reported per event type, as in JMC's Event Browser.

Built on [Quarkus](https://quarkus.io/) and the
[quarkus-mcp-server](https://github.com/quarkiverse/quarkus-mcp-server) extension. The build produces an uber-jar
(fat JAR) that runs on any JDK 25+, and can optionally also produce a
[GraalVM native image](https://www.graalvm.org/) executable. Both provide the same tools.

### Example prompts

> "Load /tmp/app.jfr and tell me what's wrong with this application."

> "Find the longest GC pause in the recording, and tell me everything about it and how it affected the execution of my
> service at that point in time."

> "This recording has a latency outlier around the 40 second mark. Work out what caused it."

## Tools

| Tool                  | Description                                                                                      |
|-----------------------|--------------------------------------------------------------------------------------------------|
| **Recordings**        |                                                                                                  |
| `getVersion`          | Returns the server version.                                                                      |
| `loadRecording`       | Load a `.jfr` file. **Call this first**. Returns a `recordingId` and a summary.                  |
| `listRecordings`      | List the currently loaded recordings.                                                            |
| `getRecordingInfo`    | Event count, event type count, duration, and stored result sets.                                 |
| `unloadRecording`     | Unload a recording and free its memory.                                                          |
| **Discovery**         |                                                                                                  |
| `getEventTypes`       | List all event types in the recording with event counts.                                         |
| `getAttributes`       | List the attributes of one event type, with identifiers and content types.                       |
| `getSharedAttributes` | Find attributes shared across event types, i.e. potential correlation paths (e.g. `gcId`).       |
| **Querying**          |                                                                                                  |
| `getEventTable`       | Events as a compact tab-separated table. The workhorse; filter by type, time, or attribute value.|
| `getJfrEvents`        | Events with every attribute on its own line. Verbose; prefer `getEventTable`.                    |
| `getTimeSeries`       | Timestamp/value pairs for one attribute. Good for locating interesting intervals.                |
| `queryEvents`         | Compound typed filters, precise structured rows, pagination, and stored selections.             |
| `exportResultSet`     | Export every page of a stored selection as precise JSON or CSV.                                 |
| **Analysis**          |                                                                                                  |
| `getRuleResults`      | Run JMC's automated analysis rules. **A cheap, low token way, to find ideas on where to look.**  |
| `aggregateEvents`     | count/sum/avg/min/max/stddev over event durations; stores the min/max events.                    |
| `aggregateGroups`     | Group by attributes and compute count, sum, average, extrema, standard deviation, median, and percentiles. |
| `compareRecordings`   | Apply identical filters and grouping to two recordings; return statistics, deltas, and percentage changes. |
| `getStackTrace`       | Stack paths with self/inclusive weights; scope by result set, thread, time, or frame.             |
| `getStackStatistics`  | Precise structured stack weights and percentages with an explicit denominator.                  |
| **Correlation**       |                                                                                                  |
| `findRelatedEvents`   | Events concurrent with, or contained within, a set of reference events.                          |
| `joinIntervals`       | Attach the enclosing interval events of given types to each event, matching thread and attributes. |
| `combineResultSets`   | `intersect` / `union` / `subtract` two stored result sets.                                       |
| `listResultSets`      | List the stored result sets and their sizes.                                                     |
| `deleteResultSet`     | Delete a stored result set.                                          s                             |

Every tool takes an optional `recordingId`. When exactly one recording is loaded it can be left empty, so a
single-recording session never has to repeat the path.

### Recording visibility

Loaded recordings, stored result sets and cached analysis live in memory in the server process. Agents connected to
the same process share them. An agent connected to a different process, which is common with STDIO clients, must call
`loadRecording` itself. Restarting the server clears all state. Use `listRecordings` and `listResultSets` to see what
the current process holds, and use distinct result set names when agents share a process.

### Result sets

Several tools accept a `storeAs` parameter, which keeps the matched events on the server under a name.
`queryEvents`, `aggregateGroups`, `getStackTrace`, `getStackStatistics`, `joinIntervals`, `exportResultSet`,
`findRelatedEvents` and `combineResultSets` can then take those names as input. This lets an agent compose a multi-step
correlation, such as "the events during the long GC pause, intersected with the events during the slow request", without
the event data ever passing through the model's context.

A suggested route through the tools:

1. `loadRecording`, then `getRuleResults` to see what JMC already knows is wrong.
2. `getEventTypes` / `getSharedAttributes` to see what the recording contains and how it can be correlated.
3. `aggregateEvents` or `getTimeSeries` to find the specific bad instance or interval.
4. `findRelatedEvents` with `storeAs`, then `combineResultSets`, to work out what coincided with it.
5. `getEventTable` or `getStackTrace` to read the detail.

### Compound filters and grouped statistics

Use `aggregateGroups` to answer grouped questions in one call. For example, substitute the event type and attribute
identifiers discovered with `getEventTypes` / `getAttributes`:

```json
{
  "eventType": "game.Terrain",
  "groupBy": "vehicle",
  "attributes": "duration,remainingSeconds",
  "filters": [
    {"attribute": "hpAtStart", "operator": "eq", "value": "0"},
    {"attribute": "iterations", "operator": "eq", "value": "6"},
    {"attribute": "remainingSeconds", "operator": "gt", "value": "0"}
  ],
  "percentiles": [50, 90, 95, 99],
  "storeAs": "terrain.filtered"
}
```

Each group includes its event count and statistics for each requested numeric attribute. Filters are AND-combined;
operators are `eq`, `ne`, `gt`, `gte`, `lt`, `lte`, `contains`, `exists` and `notExists`. Numeric operands are decimal
strings. A quantity operand uses the default unit of its quantity kind unless `unit` is given, for example
`{"attribute":"duration","operator":"gt","value":"1","unit":"ms"}`. Text equality is exact and `contains` matches a
substring. Missing and non-finite values fail every comparison, including `ne`; use `notExists` to select missing
values. `groupBy` takes comma-separated attributes; omit it for overall statistics.

Statistics use the first observed unit for each attribute within a group. Missing and non-finite values are excluded
and reported as `missingCount` and `nonFiniteCount`; `valueCount` is the number of values used. Standard deviation is
for the population. Percentiles, including the median, interpolate linearly between sorted values at `(n-1)*p/100`;
the defaults are 50, 90, 95 and 99. Statistics cover all matching events, even when only one page of groups is
returned.

`queryEvents` accepts the same filters and optional `resultSet`, returning precise rows with selected `columns`.
Both tools can store the complete filtered selection through `storeAs`, independently of their output limit.

To compare two recordings, load both and call `compareRecordings` with `baselineRecordingId` and
`candidateRecordingId`, plus the same `eventType`, `filters`, `groupBy`, `attributes` and `percentiles` shown above.
It returns the statistics for each side, candidate minus baseline deltas, and percentage changes. Groups found on
only one side are marked as such. Percentage changes from a zero or missing baseline are null. Time bounds are
relative to the start of each recording. When grouping by thread, threads are matched by name, since thread IDs
differ between runs. Counts and sums are not normalized, so compare recordings or time windows
of similar length.

### Precise results and export

`queryEvents`, `aggregateGroups`, `compareRecordings`, `getStackStatistics`, `joinIntervals` and `exportResultSet`
return MCP structured content. Numeric values include `value`, a lossless decimal `exactValue` string, a
locale-independent `unit`, and `quantityKind`. Values with a unit also include `canonicalValue`,
`canonicalExactValue` and `canonicalUnit`, expressed in the default unit of the quantity kind, so units such as ticks
can be read as seconds. Threads include `id` and `name`. Clients that parse JSON numbers as doubles should use the
exact value strings for large integers. Non-finite values have a null `value` and an `exactValue` such as `NaN` or
`Infinity`.

To export the stored selection above, call:

```json
{
  "resultSet": "terrain.filtered",
  "columns": "startTime,duration,vehicle,hpAtStart,remainingSeconds",
  "format": "json",
  "offset": 0,
  "limit": 1000
}
```

Call `exportResultSet` again with the returned `nextOffset` until it is null; `total`, `returned` and `truncated`
describe the page. `format: "csv"` returns CSV text with quantities and threads encoded as JSON cells. Rows are in
the order of the stored result set, grouped by event type, not sorted by time. The data is returned to the client;
nothing is written on the server.

### Scoped stacks and interval joins

`getStackTrace` and `getStackStatistics` accept `sourceResultSet`, `thread` (exact name or Java thread ID), and
`frameContains` (substring of a fully qualified method). With the default `denominator: "filtered"`, percentages are
relative to the selected events that have stacks and positive weights, so a query for one thread reports shares of
that thread's samples. With `denominator: "source"`, percentages are relative to the selection by event type, time
and result set, before thread and frame filtering. The result reports the denominator and the number of events with
missing stacks and missing, zero or invalid weights. Each row is a call path with self and inclusive weights and
percentages. Inclusive weights of different rows overlap and must not be summed. Count weights are counts, duration
weights are seconds and memory weights are KiB; other weights use the default unit of their quantity kind. The
`*ExactValue` strings carry every digit for clients that parse numbers as doubles.

To attach both an enclosing physics phase and tick to every selected terrain event:

```json
{
  "childResultSet": "terrain.filtered",
  "parentEventType": "game.PhysicsPhase,game.SimulationTick",
  "matchAttributes": "run",
  "sameThread": true,
  "childColumns": "vehicle,hpAtStart,duration",
  "parentColumns": "phase,tickId,run"
}
```

`joinIntervals` checks containment for each child separately, with inclusive start and end boundaries. By default the
thread must match, and keys listed in `matchAttributes`, such as `run`, must match too; a missing key never matches.
For each parent type it picks the shortest enclosing interval, then the earliest start, then source order. Each row
includes the chosen parents and the number of candidates. Children without a parent are kept by default. The summary
covers the whole join: `matchedChildren` have at least one parent, `fullyMatchedChildren` have one of every requested
type, and `unmatchedChildren` have none. Children and parents can be given as event types or stored result sets.

## Building from source

### 1. Prerequisites

| Requirement                     | Notes                                                                                                                                                         |
|---------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| JDK 25+                         | Builds the server, and runs the uber-jar. Any JDK 25+ distribution works.                                                                                     |
| JDK 17 and JDK 21 toolchains    | The JMC core build needs both. See "Java Version & Toolchains" in the [AGENTS.md](../AGENTS.md) at the repository root for the `~/.m2/toolchains.xml` layout. |
| Maven 3.9+                      |                                                                                                                                                               |
| GraalVM for JDK 25+ with `native-image` | Optional, only for the native executable. [Download](https://www.graalvm.org/downloads/); check with `native-image --version`.                        |

### 2. Get the sources

```bash
git clone https://github.com/openjdk/jmc.git
cd jmc
```

### 3. Install the JMC core libraries

This module builds against the JMC core libraries from your local Maven repository, so they have to be published there
first. From the repository root:

```bash
./build.sh --installCore
```

On Windows use `build.bat --installCore`. This installs `org.openjdk.jmc:common`,
`flightrecorder`, `flightrecorder.rules` and `flightrecorder.rules.jdk` into `~/.m2/repository`. It only needs redoing
when the core libraries change.

> If `mvn package` below fails with "Could not resolve dependencies ... org.openjdk.jmc:common", this
> step was skipped or did not finish.

### 4. Build the server

The `mcp` module is a standalone Maven project. Like `agent`, it is not part of the root reactor, so build it from its
own directory.

```bash
cd mcp
mvn package
```

That produces the runnable uber-jar:

```
mcp/target/mcp-1.0.0-SNAPSHOT-runner.jar
```

It runs on any JDK 25+; see the installation sections below for the command line. Run the tests with
`mvn verify`.

### 5. Build a native executable (optional)

The same sources can also be compiled into a native executable with GraalVM:

```bash
cd mcp
JAVA_HOME=/path/to/graalvm mvn package -Dnative -DskipTests
```

That produces an executable that needs no JDK at runtime:

```
mcp/target/mcp-1.0.0-SNAPSHOT-runner
```

On Windows the file is `mcp-1.0.0-SNAPSHOT-runner.exe`, and you also need Visual Studio 2022 with the
"Desktop development with C++" workload. The build takes a couple of minutes.

Choose whichever suits your environment. The uber-jar runs on any JDK 25+ and needs no GraalVM installation. The
native executable needs no JDK and starts faster, which some users prefer since STDIO clients start a new server
process for each session.

To run the native sanity checks against the binary you just built:

```bash
mvn verify -Dnative.image.path=target/mcp-1.0.0-SNAPSHOT-runner
```

These are skipped unless `native.image.path` is set.

## Installing in Claude Code

The simplest route is `claude mcp add`. Use the absolute path to whichever artifact you built, and note the `--`
separating Claude's own flags from the server command.

Using the uber-jar:

```bash
claude mcp add jmc -- java -Dquarkus.mcp.server.stdio.enabled=true \
  -jar /absolute/path/to/jmc/mcp/target/mcp-1.0.0-SNAPSHOT-runner.jar
```

Using the native executable:

```bash
claude mcp add jmc -- /absolute/path/to/jmc/mcp/target/mcp-1.0.0-SNAPSHOT-runner \
  -Dquarkus.mcp.server.stdio.enabled=true
```

The default scope is `local`, which registers the server privately for you in the current directory. Use `--scope user`
to make it available in all your projects, or `--scope project` to write it to a
`.mcp.json` that can be checked in and shared with a team.

Then verify it connected:

```bash
claude mcp list
```

or run `/mcp` inside Claude Code. You should see `jmc` connected, with the tools from the table above. Try it with:

> "Load /path/to/some.jfr and run the automated analysis."

To remove it again: `claude mcp remove jmc`.

For the general mechanics of MCP servers in Claude Code (scopes, project-shared `.mcp.json`, authentication,
debugging), see the
[Claude Code MCP documentation](https://docs.claude.com/en/docs/claude-code/mcp).

### Editing the config by hand

`claude mcp add` writes the entry for you, but if you would rather edit configuration directly, the equivalent JSON
for the uber-jar is:

```json
{
  "mcpServers": {
    "jmc": {
      "command": "java",
      "args": [
        "-Dquarkus.mcp.server.stdio.enabled=true",
        "-jar",
        "/absolute/path/to/jmc/mcp/target/mcp-1.0.0-SNAPSHOT-runner.jar"
      ]
    }
  }
}
```

For the native executable, set `command` to `/absolute/path/to/jmc/mcp/target/mcp-1.0.0-SNAPSHOT-runner` and keep only
`-Dquarkus.mcp.server.stdio.enabled=true` in `args`. If `java` is not on the `PATH` of the client, use the absolute
path to the `java` executable.

Put that in `.mcp.json` in a project root to share it with a team, or in `~/.claude.json` for yourself. Restart Claude
Code, or run `/mcp`, to pick up changes.

## Installing in Claude Desktop

Edit `claude_desktop_config.json` (on macOS
`~/Library/Application Support/Claude/claude_desktop_config.json`, on Windows
`%APPDATA%\Claude\claude_desktop_config.json`), add the same `mcpServers` entry as above, then restart Claude
Desktop.

On Windows, Claude Desktop may launch the server with a working directory it cannot write to, which makes Quarkus fail
while scanning for config files. If that happens, add
`-Dquarkus.config.locations=.` and `-Duser.dir=C:\some\writable\dir` to `args`.

## Notes and limitations

- **Recordings are held in memory.** `loadRecording` parses the whole file, and it stays resident until
  `unloadRecording`. A multi-gigabyte recording needs a correspondingly large heap.
- **Rule results are cached** per recording, and computed on the first `getRuleResults` call. On a large recording that
  first call can take a while.
- **Result sets are per recording** and compared by event identity, so `combineResultSets` only makes sense for sets
  derived from the same loaded recording.
- **Event data is untrusted.** Thread names, class names, stack frames and log messages in a recording come from the
  profiled application. The tool descriptions tell the model not to follow instructions found in event data, but treat
  anything surfaced from a recording as data.
- **Output is capped.** Every listing tool has a default and a hard cap on how much it returns, and says so when it
  truncates. Cross-type queries lead with an event type breakdown so a truncated table never hides a whole event type.
  The structured query, grouping and export tools are paged, and `storeAs` always keeps the complete selection.
  Percentiles hold and sort every value of a group in memory, so large selections need more heap.

## Troubleshooting

- **No log output**: logs go to `jmc-mcp-server.log` in the working directory, not to stdout, so they cannot corrupt the
  STDIO transport. Check that file first.
- **Server disconnects immediately**: make sure `-Dquarkus.mcp.server.stdio.enabled=true` is in
  `args`, and before `-jar` when using the uber-jar.
- **`Unknown recording`**: call `listRecordings`, or pass the path returned by `loadRecording`. Recordings are keyed by
  canonical path, so any spelling of the same file resolves to one recording.
- **Build fails resolving `org.openjdk.jmc` artifacts**: run `./build.sh --installCore` from the jmc root first. This
  module builds against the core libraries from the local Maven repository, like
  `agent` does.
