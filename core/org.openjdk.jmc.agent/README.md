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

## Interacting with the agent
At runtime the agent can be used to modify the transformed state of a class. To specify a desired state, supply the defineEventProbes function with a XML description of event probes to add, keep or modify, and leave out all those that should be reverted to their preinstrumentation versions.

### Using a security manager
When running with a security manager, the 'control' Management Permission must be granted to control the agent through the MBean. To set fine grained permissions for authenticated remote users, see [here](https://docs.oracle.com/javadb/10.10.1.2/adminguide/radminjmxenablepolicy.html#radminjmxenablepolicy) and [here](https://docs.oracle.com/javase/7/docs/technotes/guides/management/agent.html#gdeup).

## Known Issues
* The full converter support is still to be merged into the open source repo
* Support for emitting an event only on exception has yet to be implemented
* Support for reflective access to fields has yet to be implemented
* Support for emitting event even though an exception was raised in a called method (try-finally)
* XML probe definition validation (schema)
* Support for redefinitions and controlling the agent over JMX is not yet completed