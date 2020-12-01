# The JMC Agent
The JMC agent allows users to add JFR instrumentation declaratively to a running program. The agent can, for example, be used to add flight recorder events to third party code for which the source is not available.

To build the agent you will need a JDK 11 or later. To run the agent, a JDK 8 or later will be needed.

## Building the agent
To build the agent, simply use maven in the agent folder.

```bash
mvn clean package
```

## Running the agent
The agent can be tried out using the included example program.

Here is an example for running the example program with OpenJDK 8 (u272+):

```bash
java -XX:+FlightRecorder -javaagent:target/org.openjdk.jmc.agent-1.0.0-SNAPSHOT.jar=target/test-classes/org/openjdk/jmc/agent/test/jfrprobes_template.xml -cp target/org.openjdk.jmc.agent-1.0.0-SNAPSHOT.jar:target/test-classes/ org.openjdk.jmc.agent.test.InstrumentMe
```

Here is an example for running the example program with OpenJDK 11+ and the Oracle JDK 11+:

```bash
java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED -XX:+FlightRecorder -javaagent:target/org.openjdk.jmc.agent-1.0.0-SNAPSHOT.jar=target/test-classes/org/openjdk/jmc/agent/test/jfrprobes_template.xml -cp target/org.openjdk.jmc.agent-1.0.0-SNAPSHOT.jar:target/test-classes/ org.openjdk.jmc.agent.test.InstrumentMe
```

Here is an example for running the example program with Oracle JDK 8:

```bash
java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -javaagent:target/org.openjdk.jmc.agent-1.0.0-SNAPSHOT.jar=target/test-classes/org/openjdk/jmc/agent/test/jfrprobes_template.xml -cp target/org.openjdk.jmc.agent-1.0.0-SNAPSHOT.jar:target/test-classes/ org.openjdk.jmc.agent.test.InstrumentMe
```
Note: converters are not supported on Oracle JDK 8.

## Interacting with the agent
At runtime the agent can be used to modify the transformed state of a class. To specify a desired state, supply the defineEventProbes function with an XML description of event probes to add, keep or modify, and leave out all those that should be reverted to their pre-instrumentation versions.

### Using a security manager
When running with a security manager, the 'control' Management Permission must be granted to control the agent through the MBean. To set fine grained permissions for authenticated remote users, see [here](https://docs.oracle.com/javadb/10.10.1.2/adminguide/radminjmxenablepolicy.html#radminjmxenablepolicy) and [here](https://docs.oracle.com/javase/7/docs/technotes/guides/management/agent.html#gdeup).

## Known Issues
* The full converter support is still to be merged into the open source repo
* Support for emitting an event only on exception has yet to be implemented
