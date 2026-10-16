# The JMC Agent
The JMC agent allows users to add JFR instrumentation declaratively to a running program. The agent can, for example, be used to add flight recorder events to third party code for which the source is not available.

To build and run the agent you will need JDK 17 or later.

## Building the agent
To build the agent, simply use maven in the agent folder.

```bash
mvn clean package
```

## Trying out the agent
The agent can be tried out using the demo programs included with the tests; they are built together
with the agent. Run the commands below from the `agent` folder, on JDK 17 or later. Note that the
agent jar does not need to be on the classpath (the `-javaagent` option appends it to the system
class path by itself), and that `--add-opens java.base/jdk.internal.misc=ALL-UNNAMED` is no longer
required: the agent opens the package it needs to define the generated JFR event classes by
itself.

Each demo starts a flight recording from the command line and dumps it to a file on exit, so you
can simply open the resulting recording in JMC afterwards. You can of course also skip the
recording options and instead connect to the running demo with JMC to start a recording, watch the
events live, or play with the agent configuration in the Agent tab.

For Eclipse users, there are ready-to-use launch configurations for the demos in the `launchers`
folder.

### The instrumentation demo
`InstrumentMe` continuously runs through a set of example methods, and the probe definitions in
`jfrprobes_template.xml` demonstrate the different ways to instrument them, such as timing methods and
capturing parameters, return values, fields and expressions:

```bash
java -XX:StartFlightRecording=filename=instrumentme.jfr,dumponexit=true -javaagent:target/agent-1.1.0-SNAPSHOT.jar=target/test-classes/org/openjdk/jmc/agent/test/jfrprobes_template.xml -cp target/test-classes/ org.openjdk.jmc.agent.test.InstrumentMe
```

The demo runs until you press `<enter>`, then dumps the recording to `instrumentme.jfr`. Open it in
JMC and look in the Event Browser for the *JFR Hello World Event* events, under the *demo* category.

### The converter demo
`InstrumentMeConverter` demonstrates converters, which convert arbitrary types (here the `Gurka`
demo class) into values that can be captured in JFR events:

```bash
java -XX:StartFlightRecording=filename=converters.jfr,dumponexit=true -javaagent:target/agent-1.1.0-SNAPSHOT.jar=target/test-classes/org/openjdk/jmc/agent/converters/test/jfrprobes_template.xml -cp target/test-classes/ org.openjdk.jmc.agent.converters.test.InstrumentMeConverter
```

Like the instrumentation demo, it runs until you press `<enter>`. The *ConverterEvent* events are
under the *demo* category, with the `Gurka` parameters captured through the different converters.

### The collection leak demo
`CollectionLeakDemo` slowly leaks entries into several different kinds of collections. For each
collection, one in every N (default 100) added entries is retained, so the collections grow without
bound and periodically resize their backing arrays. Run it with
[collection resize tracking](#collection-resize-tracking) enabled to see the
`jdk.jmc.CollectionResize` events this produces:

```bash
java -Dgurka.record=collection-leak.jfr -javaagent:target/agent-1.1.0-SNAPSHOT.jar=target/test-classes/org/openjdk/jmc/agent/test/collectiontracking_enabled.xml -cp target/test-classes/ org.openjdk.jmc.agent.test.CollectionLeakDemo 10
```

Let it run for half a minute or so (events start appearing once the collections pass the
configured `minsize` threshold of 128 entries), then press `<enter>` to stop it and dump the
recording to `collection-leak.jfr` (the demo starts the recording itself when `-Dgurka.record` is
set). In JMC, the events are in the Event Browser under *JMC Agent / Collections*: the leak
signature is the same collection identity resizing over and over with an ever-growing entry count,
and the event stack traces point at the code doing the leaking.

The trailing argument is the leak rate: leak one in every N added entries. The example uses 10 so
that you quickly get plenty of events; drop it (the default is 100) for a slower, more realistic
leak. `-Dgurka.leak.durationMillis=30000` runs the demo headless for 30 seconds instead of waiting
for `<enter>`.

### Loading the agent into a running process from JMC
The examples above pass the agent and the recording options on the command line, but neither is
required: the JMC Agent plug-in in JMC can load the agent into an already-running JVM. Start any of
the demos completely plain, for example:

```bash
java -cp target/test-classes/ org.openjdk.jmc.agent.test.CollectionLeakDemo 10
```

Then, in JMC:

1. Find the demo process in the JVM Browser, expand it, and double-click the *JMC Agent* node.
Since no agent is running in the process yet, this opens the *Start JMC Agent* wizard.
2. Point *Agent JAR* at the built `agent/target/agent-1.1.0-SNAPSHOT.jar`. Optionally point
*Agent XML* at a configuration, for example
`target/test-classes/org/openjdk/jmc/agent/test/collectiontracking_enabled.xml`, or
`jfrprobes_template.xml` for the InstrumentMe demo, or leave it empty and push a configuration
later. Click *Start*.
3. The *Agent Live Config* editor opens, showing the live instrumentation in the process. From
here you can edit probes, load and save presets, and apply new configurations to the running
process.
4. Start a flight recording on the same JVM to see the events arrive.

No special options are needed on the demo's command line for this: when the agent is loaded
dynamically it opens the `jdk.internal.misc` package itself, so not even the `--add-opens` option
is required. The target JVM will log a "Java agent has been loaded dynamically" warning; start it
with `-XX:+EnableDynamicAgentLoading` to acknowledge and silence it (a future JDK release is
expected to require the flag for dynamic loading). Note that the agent can only be loaded this way
into JVMs on the local machine, and that enabling collection tracking in an already-running
process retransforms hot core classes (see the note on deoptimization in the
[collection resize tracking](#collection-resize-tracking) section).

## Collection resize tracking
In addition to the declarative JFR probes, the agent has a built-in capability aimed at two things:

* Right-sizing: finding good starting sizes (initial capacities) for your collections so you can
pre-size them and avoid a lot of resizing of the array backing the collection (and the array copying
and rehashing that each resize incurs); and
* leak hunting: spotting slowly leaking collections that keep growing without bound.

It emits a `jdk.jmc.CollectionResize` event (category *JMC Agent / Collections*) whenever a collection's backing array is resized: a collection that resizes repeatedly
is a candidate for a larger initial capacity, while one that keeps growing across the run is a likely
memory leak. Each event carries the collection's identity and a stack trace, so you can identify
which collection is responsible and where to pre-size it, or stop leaking into it. The currently
tracked collections are `HashMap` (which also covers `LinkedHashMap`, `HashSet` and `LinkedHashSet`),
`ArrayList`, `Vector`, `Hashtable`, `PriorityQueue` and `ArrayDeque`.

It is enabled by adding a `collectiontracking` element to the agent configuration:

```xml
<jfragent>
	<collectiontracking minsize="128"/>
</jfragent>
```

The only tunable is `minsize` (default 128): the minimum collection size (entry count) that must be
reached before a resize event is emitted, which suppresses the noise from small collections resizing
early in their life. Each event records the collection's runtime class, a stable identity hash of the
instance (to correlate resizes of the same collection over time), the entry count, and the old and
new capacities.

The easiest way to see the capability in action is the
[collection leak demo](#the-collection-leak-demo). The event is enabled by default, so any recording
will capture it without further JFR configuration.

Collection tracking is best enabled at agent startup (`premain`). It instruments hot core JDK
classes, so the one-time weave is cheap at startup, but enabling it at runtime, via dynamic attach
(`agentmain`) or by pushing a `collectiontracking` element over JMX (see below), retransforms
already-JIT-compiled core classes and can cause a lot of deoptimization. It works, but startup is
the better choice.

Unlike the JFR probes, which are declarative so that probes omitted from a config push are
reverted, collection tracking is sticky: a push that omits the `collectiontracking` element leaves the
capability unchanged. This keeps it compatible with clients that predate the capability (or that
drop unknown config elements): they cannot accidentally unweave it (and cause another round of
deoptimization) by pushing an unrelated probe change. In summary:

| Push contains | Effect |
|---|---|
| no `collectiontracking` element | no change |
| `<collectiontracking/>` (or `enabled="true"`) | enable; keep the live `minsize` |
| `<collectiontracking minsize="N"/>` | enable; set/retune `minsize` to N |
| `<collectiontracking enabled="false"/>` | disable and unweave |
| empty config (full revert) | disable and unweave, along with all probes |

Because an omitted element no longer describes the tracking state, the configuration read back from
the agent (`retrieveEventProbes`) always renders the effective state explicitly, as
`<collectiontracking enabled="..." minsize="..."/>`. The read-back therefore reflects the actual
state, and reading back, editing and re-pushing a configuration does not retransform the
collection classes.

When disabling at runtime, it is usually better to disable the `jdk.jmc.CollectionResize` event in
the recording than to push `enabled="false"`. Unweaving retransforms the same hot core classes
again, whereas toggling the event is free. The
instrumentation is otherwise harmless when the event is disabled (a quick threshold check per
resize).

## Interacting with the agent
At runtime the agent can be used to modify the transformed state of a class. To specify a desired state, supply the defineEventProbes function with an XML description of event probes to add, keep or modify, and leave out all those that should be reverted to their pre-instrumentation versions.

### Using a security manager
When running with a security manager, the 'control' Management Permission must be granted to control the agent through the MBean. To set fine grained permissions for authenticated remote users, see [here](https://docs.oracle.com/javadb/10.10.1.2/adminguide/radminjmxenablepolicy.html#radminjmxenablepolicy) and [here](https://docs.oracle.com/javase/7/docs/technotes/guides/management/agent.html#gdeup).

## Known Issues
* The full converter support is still to be merged into the open source repo
* Support for emitting an event only on exception has yet to be implemented
