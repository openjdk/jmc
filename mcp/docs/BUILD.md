# Building from source

[Back to the README](../README.md)

## 1. Prerequisites

| Requirement                             | Notes                                                                                                                                                            |
|-----------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| JDK 25+                                 | Builds the server, and runs the uber-jar. Any JDK 25+ distribution works.                                                                                        |
| JDK 17 and JDK 21 toolchains            | The JMC core build needs both. See "Java Version & Toolchains" in the [AGENTS.md](../../AGENTS.md) at the repository root for the `~/.m2/toolchains.xml` layout. |
| Maven 3.9+                              |                                                                                                                                                                  |
| GraalVM for JDK 25+ with `native-image` | Optional, only for the native executable. [Download](https://www.graalvm.org/downloads/); check with `native-image --version`.                                   |

## 2. Install the JMC core libraries

Assuming you have cloned the jmc repo, build the JMC core libraries. From the repository root:

```bash
./build.sh --installCore
```

On Windows use `build.bat --installCore`. This installs `org.openjdk.jmc:common`,
`flightrecorder`, `flightrecorder.rules` and `flightrecorder.rules.jdk` into `~/.m2/repository`. Redo
when the core libraries change.

> If `mvn package` below fails with "Could not resolve dependencies ... org.openjdk.jmc:common", this
> step was skipped or did not finish.

## 3. Build the server

The `mcp` module is a standalone Maven project, so build it from its own directory.

```bash
cd mcp
mvn package
```

That produces the runnable uber-jar at this path relative to the repository root:

```
mcp/target/mcp-1.0.0-SNAPSHOT-runner.jar
```

It runs on any JDK 25+; see [client setup](CLIENTS.md) for the command line. Run the tests from the `mcp` directory with
`mvn verify`.

## 4. Build a native executable (optional)

The MCP can also be compiled into a native executable with GraalVM. From the `mcp` directory:

```bash
JAVA_HOME=/path/to/graalvm mvn package -Dnative -DskipTests
```

That produces an executable that needs no JDK at runtime, at this path relative to the repository root:

```
mcp/target/mcp-1.0.0-SNAPSHOT-runner
```

On Windows the file is `mcp-1.0.0-SNAPSHOT-runner.exe`, and you also need Visual Studio 2022 with the
"Desktop development with C++" workload. The build takes a couple of minutes.

Choose whichever suits your environment. The uber-jar runs on any JDK 25+ and needs no GraalVM installation. The
native executable needs no JDK and starts faster, which some users prefer since STDIO clients start a new server
process for each session.

To run the native sanity checks against the binary you just built, from the `mcp` directory:

```bash
mvn verify -Dnative.image.path=target/mcp-1.0.0-SNAPSHOT-runner
```

These are skipped unless `native.image.path` is set.

## Troubleshooting

- **Build fails resolving `org.openjdk.jmc` artifacts**: run `./build.sh --installCore` from the JMC repository root
  first. This module builds against the core libraries from the local Maven repository, like `agent` does.
