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
| `getHistogram`        | Exact distribution of one attribute: log or linear buckets, percentiles with tail counts, JFR thresholds. |
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
`queryEvents`, `aggregateGroups`, `getHistogram`, `getStackTrace`, `getStackStatistics`, `joinIntervals`,
`exportResultSet`, `findRelatedEvents` and `combineResultSets` can then take those names as input. This lets an agent compose a multi-step
correlation, such as "the events during the long GC pause, intersected with the events during the slow request", **without
the event data ever passing through the model's context**.

## Getting started

Build the uber-jar or native executable using the [build instructions](docs/BUILD.md), then register it with your MCP
client. For example, using the uber-jar with Claude Code:

```bash
claude mcp add jmc -- java -Dquarkus.mcp.server.stdio.enabled=true \
  -jar /absolute/path/to/jmc/mcp/target/mcp-1.0.0-SNAPSHOT-runner.jar
```

Run `claude mcp list` or `/mcp` inside Claude Code to check the connection, then try:

> "Load /path/to/some.jfr and run the automated analysis."

The recording must be accessible to the server process. Use an absolute path to the `.jfr` file.

For the native executable, Claude Desktop, manual configuration and troubleshooting, see
[client setup](docs/CLIENTS.md).

## Documentation

- [Tool guide](docs/TOOLS.md): suggested workflow, filters, grouped statistics, comparisons, export, stacks, interval joins
  and limitations.
- [Building from source](docs/BUILD.md): prerequisites, core libraries, uber-jar, native executable and tests.
- [Client setup](docs/CLIENTS.md): Claude Code, Claude Desktop, configuration and troubleshooting.
