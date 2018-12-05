# The JMC Agent
The JMC agent is an agent currently under development to add JFR instrumentation declaratively to a running program. The agent can, for example, be used to add flight recorder events to third party code for which the source is not available.

To build the agent you will need a JDK 7 or later. To run the agent, a JDK 7 or later will be needed as well.

## Building the agent
To build the agent, simply use maven in the agent folder. Since the agent is not ready for prime time yet, it is not built with the rest of the core libraries.

```bash
mvn clean package
```

## Running the agent
The agent can be tried out using the included example program.

Here is an example for running the example program with Oracle JDK 7 to Oracle JDK 10:

```bash
java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -javaagent:target/org.openjdk.jmc.agent-1.0.0-SNAPSHOT.jar=target/test-classes/org/openjdk/jmc/agent/test/jfrprobes_template.xml -cp target/org.openjdk.jmc.agent-1.0.0-SNAPSHOT.jar:target/test-classes/ org.openjdk.jmc.agent.test.InstrumentMe
```

Here is an example for running the example program with OpenJDK 11+:

```bash
java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED -XX:+FlightRecorder -javaagent:target/org.openjdk.jmc.agent-1.0.0-SNAPSHOT.jar=target/test-classes/org/openjdk/jmc/agent/test/jfrprobes_template.xml -cp target/org.openjdk.jmc.agent-1.0.0-SNAPSHOT.jar:target/test-classes/ org.openjdk.jmc.agent.test.InstrumentMe
```

## Known Issues
* The full converter support is still to be merged into the open source repo
* Support for emitting an event only on exception has yet to be implemented
* Support for reflective access to fields has yet to be implemented
* Support for emitting event even though an exception was raised in a called method (try-finally)
* XML probe definition validation (schema)
* Support for redefinitions and controlling the agent over JMX is not yet completed