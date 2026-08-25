# JMC MCP Server

An MCP ([Model Context Protocol](https://modelcontextprotocol.io/)) server that exposes the JDK Mission Control flight
recording analysis capabilities as tools for LLM clients like Claude.

It is the headless sibling of the JMC AI view: the same kind of analysis, but driven from any MCP client instead of from
inside the JMC UI. Where the AI view reads the recording open in the active editor, this server loads `.jfr` files from
disk, so it works in a terminal, in CI, or anywhere else an agent runs.

Built on [Quarkus](https://quarkus.io/) and the
[quarkus-mcp-server](https://github.com/quarkiverse/quarkus-mcp-server) extension, and compiled ahead of time
with [GraalVM native image](https://www.graalvm.org/) so it starts in milliseconds and needs no JDK on the machine
running it.

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
| **Analysis**          |                                                                                                  |
| `getRuleResults`      | Run JMC's automated analysis rules. **A cheap, low token way, to find ideas on where to look.**  |
| `aggregateEvents`     | count/sum/avg/min/max/stddev over event durations; stores the min/max events.                    |
| `getStackTrace`       | Aggregated stack trace tree (flame graph data), weighted by count, duration, allocation, or I/O. |
| **Correlation**       |                                                                                                  |
| `findRelatedEvents`   | Events concurrent with, or contained within, a set of reference events.                          |
| `combineResultSets`   | `intersect` / `union` / `subtract` two stored result sets.                                       |
| `listResultSets`      | List the stored result sets and their sizes.                                                     |
| `deleteResultSet`     | Delete a stored result set.                                                                       |

Every tool takes an optional `recordingId`. When exactly one recording is loaded it can be left empty, so a
single-recording session never has to repeat the path.

### Result sets

Several tools accept a `storeAs` parameter, which keeps the matched events on the server under a name.
`findRelatedEvents` and `combineResultSets` can then take those names as input. This lets an agent compose a multi-step
correlation, such as "the events during the long GC pause, intersected with the events during the slow request", without
the event data ever passing through the model's context.

A suggested route through the tools:

1. `loadRecording`, then `getRuleResults` to see what JMC already knows is wrong.
2. `getEventTypes` / `getSharedAttributes` to see what the recording contains and how it can be correlated.
3. `aggregateEvents` or `getTimeSeries` to find the specific bad instance or interval.
4. `findRelatedEvents` with `storeAs`, then `combineResultSets`, to work out what coincided with it.
5. `getEventTable` or `getStackTrace` to read the detail.

## Building from source

### 1. Prerequisites

| Requirement                     | Notes                                                                                                                                                         |
|---------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| JDK 21+                         | Needed for the uber-jar. [Adoptium](https://adoptium.net/) builds work fine.                                                                                  |
| JDK 17 and JDK 21 toolchains    | The JMC core build needs both. See "Java Version & Toolchains" in the [AGENTS.md](../AGENTS.md) at the repository root for the `~/.m2/toolchains.xml` layout. |
| Maven 3.9+                      |                                                                                                                                                               |
| GraalVM 21+ with `native-image` | Only for the native binary. [Download](https://www.graalvm.org/downloads/); check with `native-image --version`.                                              |

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

Run the tests with `mvn verify`.

### 5. Build the native image (recommended)

```bash
cd mcp
JAVA_HOME=/path/to/graalvm mvn package -Dnative -DskipTests
```

That produces a self-contained executable needing no JDK at runtime:

```
mcp/target/mcp-1.0.0-SNAPSHOT-runner
```

On Windows the file is `mcp-1.0.0-SNAPSHOT-runner.exe`, and you also need Visual Studio 2022 with the
"Desktop development with C++" workload. The build takes a couple of minutes.

The native binary starts in about 20 ms against about 580 ms for the JVM, and MCP clients start a fresh server process
per session, so this is the version worth installing.

To run the native sanity checks against the binary you just built:

```bash
mvn verify -Dnative.image.path=target/mcp-1.0.0-SNAPSHOT-runner
```

These are skipped unless `native.image.path` is set.

## Installing in Claude Code

The simplest route is `claude mcp add`. Use the absolute path to whichever artifact you built, and note the `--`
separating Claude's own flags from the server command:

```bash
claude mcp add jmc -- /absolute/path/to/jmc/mcp/target/mcp-1.0.0-SNAPSHOT-runner \
  -Dquarkus.mcp.server.stdio.enabled=true
```

Using the uber-jar instead:

```bash
claude mcp add jmc -- java -Dquarkus.mcp.server.stdio.enabled=true \
  -jar /absolute/path/to/jmc/mcp/target/mcp-1.0.0-SNAPSHOT-runner.jar
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

`claude mcp add` writes the entry for you, but if you would rather edit configuration directly, the equivalent JSON is:

```json
{
  "mcpServers": {
    "jmc": {
      "command": "/absolute/path/to/jmc/mcp/target/mcp-1.0.0-SNAPSHOT-runner",
      "args": [
        "-Dquarkus.mcp.server.stdio.enabled=true"
      ]
    }
  }
}
```

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
