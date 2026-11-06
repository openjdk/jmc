# Configuring MCP clients

[Back to the README](../README.md)

These examples use artifacts produced by the [build instructions](BUILD.md).
Use absolute paths so the client can start the server from any working directory.

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

or run `/mcp` inside Claude Code. You should see `jmc` connected, with the tools listed in the
[README](../README.md#tools). Try it with:

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

## Troubleshooting

- **No log output**: logs go to `jmc-mcp-server.log` in the working directory, not to stdout, so they cannot corrupt the
  STDIO transport. Check that file first.
- **Server disconnects immediately**: make sure `-Dquarkus.mcp.server.stdio.enabled=true` is in
  `args`, and before `-jar` when using the uber-jar.
- **`Unknown recording`**: call `listRecordings`, or pass the path returned by `loadRecording`. Recordings are keyed by
  canonical path, so any spelling of the same file resolves to one recording.
