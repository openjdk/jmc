# Mission Control

Mission Control is an open source production time profiling and diagnostics tool for Java.

Builds of Mission Control can currently be found in the Oracle JDK on supported platforms and in the Eclipse marketplace. 

For more information on Mission Control, see https://www.oracle.com/missioncontrol.

## Downloading Builds
Binary distributions of JDK Mission Control are provided by different downstream vendors.

### Eclipse Adoptium
* Released version
* EA builds of upcoming release
* Downloadable Eclipse update site archive

[https://adoptium.net/jmc](https://adoptium.net/jmc)


### Azul (Zulu Mission Control)
* Released version

[https://www.azul.com/products/zulu-mission-control](https://www.azul.com/products/zulu-mission-control)


### Bell-Soft (Liberica Mission Control)
* Released version

[https://bell-sw.com/pages/lmc](https://bell-sw.com/pages/lmc)

### Oracle
* Released version
* Integrated (in-app) update site
* Eclipse update site

[https://www.oracle.com/java/technologies/jdk-mission-control.html](https://www.oracle.com/java/technologies/jdk-mission-control.html)

### Red Hat
* Released version

Red Hat distributes JDK Mission Control in Red Hat Enterprise Linux (RHEL). JMC is available in RHEL 7 as the rh-jmc Software Collection, and is provided in RHEL 8 by the jmc:rhel8 module stream. JMC is also included in the OpenJDK [developer builds](https://developers.redhat.com/products/openjdk/download) for Windows.

## Mission Control Features

### Application Features

* A framework for hosting various useful Java tools 

* A tool for visualizing the contents of Java flight recordings, and the results of an automated analysis of the contents

* A JMX Console 

* A tool for heap waste analysis

### Core API Features

* Core APIs for parsing and processing Java flight recordings 

* Core API can *read* recordings from JDK 7 and above

* Core API can *run* on JDK 8 and above

* Core API contains a framework for handling units of measurement and physical quantities

* Core API supports headless analysis of Java flight recordings


### Core API Example

Example for producing an HTML report from the command line:

```bash
java -cp <the built core jars> org.openjdk.jmc.flightrecorder.rules.report.html.JfrHtmlRulesReport <file> [<outputfile>]
```


Example for finding the standard deviation for the java monitor events in a recording:

```java
import java.io.File;
 
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
 
/**
 * Finds out the standard deviation for the java monitor enter events.
 */
public class LoadRecording {
    public static void main(String[] args) throws Exception {         
        IItemCollection events = JfrLoaderToolkit.loadEvents(new File(args[0]));
        IQuantity aggregate = events.apply(ItemFilters.type(JdkTypeIDs.MONITOR_ENTER))
                .getAggregate(Aggregators.stddev(JfrAttributes.DURATION));
         
        System.out.println("The standard deviation for the Java monitor enter events was "
                + aggregate.displayUsing(IDisplayable.AUTO));
    }
}
```


Example for programmatically running the rules:

```java
import java.io.File;
import java.util.concurrent.RunnableFuture;
 
import org.example.util.DemoToolkit;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
 
public class RunRulesOnFileSimple {
    public static void main(String[] args) throws Exception {
        File recording = DemoToolkit.verifyRecordingArgument(RunRulesOnFileSimple.class, args);
        IItemCollection events = JfrLoaderToolkit.loadEvents(recording);
         
        for (IRule rule : RuleRegistry.getRules()) {
            RunnableFuture<Result> future = rule.evaluate(events, IPreferenceValueProvider.DEFAULT_VALUES);
            future.run();
            Result result = future.get();
            if (result.getScore() > 50) {
                System.out.println(String.format("[Score: %3.0f] Rule ID: %s, Rule name: %s, Short description: %s",
                        result.getScore(), result.getRule().getId(), result.getRule().getName(),
                        result.getShortDescription()));
            }
        }
    }
}
```


Example for programmatically running rules in parallel:

```java
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
 
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
 
/**
 * Runs the rules on the events in the specified file in parallel, then prints
 * them in order of descending score.
 */
public class RunRulesOnFile {
    private final static Executor EXECUTOR = Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
    private static int limit;
 
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println(
                    "Usage: RunRulesOnFile <recording file> [<limit>]\n\tThe recording file must be a flight recording from JDK 7 or above. The limit, if set, will only report rules triggered with a score higher or equal than the limit.");
            System.exit(2);
        }
        IItemCollection events = JfrLoaderToolkit.loadEvents(new File(args[0]));
        if (args.length > 1) {
            limit = Integer.parseInt(args[1]);
        }
        Stream<RunnableFuture<Result>> resultFutures = RuleRegistry.getRules().stream()
                .map((IRule r) -> evaluate(r, events));
        List<Result> results = resultFutures.parallel().map((RunnableFuture<Result> runnable) -> get(runnable))
                .collect(Collectors.toList());
        results.sort((Result r1, Result r2) -> Double.compare(r2.getScore(), r1.getScore()));
        results.stream().forEach(RunRulesOnFile::printResult);
    }
 
    public static RunnableFuture<Result> evaluate(IRule rule, IItemCollection events) {
        RunnableFuture<Result> evaluation = rule.evaluate(events, IPreferenceValueProvider.DEFAULT_VALUES);
        EXECUTOR.execute(evaluation);
        return evaluation;
    }
 
    public static Result get(RunnableFuture<Result> resultFuture) {
        try {
            return resultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
 
    private static void printResult(Result result) {
        if (result.getScore() >= limit) {
            System.out.printf("(%.0f) [%s]: %s\nDetails:\n%s\n============<End of Result>============\n",
                    result.getScore(), result.getRule().getId(), result.getShortDescription(),
                    result.getLongDescription() == null ? "<no description>" : result.getLongDescription());
        }
    }
}
```

## Building Mission Control from Source

Prerequisites for building Mission Control:

1. Install a JDK 17 distribution and make sure it is declared in the local maven toolchain `~/.m2/toolchains.xml`

2. Install a JDK 21 distribution and make sure that it too is declared in the local maven toolchain.

3. Install Maven (version 3.5.x. or above)

On Linux or macOS you can use the `build.sh` script to build JMC:
```
usage: call ./build.sh with the following options:
   --installCore to install the core artifacts
   --test        to run the tests
   --testUi      to run the tests including UI tests
   --packageJmc  to package JMC
   --clean       to run maven clean
```

Otherwise follow the steps manually.

## Building JMC Step-by-Step

Here are the individual steps:

1. Get the third-party dependencies into a local _p2_ repo and make it available on localhost. 

2. Build and install the core libraries.

3. Build the JMC application.

First, if on Mac / Linux:

```bash
mvn p2:site --file releng/third-party/pom.xml; mvn jetty:run --file releng/third-party/pom.xml
```

Or, if on Windows:

```bash
mvn p2:site --file releng\third-party\pom.xml && mvn jetty:run --file releng\third-party\pom.xml
```

Then in another terminal (in the project root):

```bash
mvn clean install --file core/pom.xml # Install JMC's flight recorder libraries
mvn package # Package JMC
```

If maven reports a toolchain error, e.g. :

```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-toolchains-plugin:3.0.0:toolchain (default) on project missioncontrol: Cannot find matching toolchain definitions for the following toolchain types:
[ERROR] jdk [ version='17' ]
[ERROR] Please make sure you define the required toolchains in your ~/.m2/toolchains.xml file.
```

Create or amend the local maven toolchain file by pointing to the right/any JDK 17.

<details><summary><code>~/.m2/toolchains.xml</code></summary>

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <!-- JDK toolchains -->
  <!-- 
    Declare the JDK 17 toolchain installed on the local machine, 
    make sure the id is : JavaSE-17

    Tycho needs this to find the right _execution environment_.
  -->
  <toolchain>
    <type>jdk</type>
    <provides>
      <id>JavaSE-17</id>
      <version>17</version>
      <vendor>amazon</vendor>
    </provides>
    <configuration>
      <jdkHome>/Users/brice.dutheil/.asdf/installs/java/corretto-17.0.7.7.1</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

</details>

Note that you may need to define proxy settings if you happen to be behind a firewall. In your `~/.m2/settings.xml` file (if you have none, simply create one), add:

```xml
<settings>
  <proxies>
    <proxy>
      <id>http-proxy</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>my.proxy.example.org</host>
      <port>80</port>
      <nonProxyHosts>localhost|*.example.org</nonProxyHosts>
    </proxy>
    <proxy>
      <id>https-proxy</id>
      <active>true</active>
      <protocol>https</protocol>
      <host>my.proxy.example.org</host>
      <port>80</port>
      <nonProxyHosts>localhost|*.example.org</nonProxyHosts>
    </proxy>
  </proxies>
</settings>
```

## Running Tests
To run the unit tests:

```bash
mvn verify
```

To run the UI tests:

```bash
mvn verify -P uitests
```
Note that the UI tests will take some time to run, and that you need to stop interacting with your computer for the duration of the tests.

Spotbugs can take some time to run. If you are only interested in the test results, you can skip running spotbugs by setting `-Dspotbugs.skip=true`.

For example:

```bash
mvn verify -P uitests -Dspotbugs.skip=true
```

## Filtering Test Runs
Aside from the from the simple -test Maven flag test classes that should be run/not run can be specified by means of the system properties "test.includes" and/or "test.excludes". Multiple patterns can be specified by comma separation.

For example:

```bash
mvn verify -Dtest.includes=**/*TestRulesWithJfr*,**/*StacktraceModelTest*
```

When specifying both test.includes and "test.excludes" the test.excludes takes precedence and filters out tests that also are matched by "test.includes".

For example:

```bash
mvn verify -P uitests -Dtest.includes=**/*SystemTabTest*,**/*TestRulesWithJfr*,**/*StacktraceModelTest* -Dtest.excludes=**/*ModelTest*
```

The above will not run `StacktraceModelTest`, as that is also matched by `test.excludes`.

Note that if UI-tests are supposed to be part of the filtered run the `uitests` profile needs to be specified as well. Otherwise the UI won't start up and so the tests fail.


## Building using docker and docker-compose

```
docker-compose -f docker/docker-compose.yml run jmc
```

Once build has finished the results will be in the `target` directory

## Running the Locally Built JMC
The built JMC will end up in the `target` folder in the root. The launcher is located in `target/products/org.openjdk.jmc/<platform>`. By default whichever JRE is on the path 
will be used. Remember to set it to a JDK (rather than a JRE) if you want the launched mission control to automatically discover locally running JVMs. To override which JVM 
to use when launching, add `-vm` and the path to a directory where a JDK java launcher is located, for example `-vm $JAVA_HOME/bin`.

Here is an example for Mac OS X:

```bash
# on aarch64 (M1/M2/M3/M4)
target/products/org.openjdk.jmc/macosx/cocoa/aarch64/JDK\ Mission\ Control.app/Contents/MacOS/jmc

# on x86_64 (Intel)
target/products/org.openjdk.jmc/macosx/cocoa/x86_64/JDK\ Mission\ Control.app/Contents/MacOS/jmc

```

Here is an example for Linux:

```bash
# on x86_64
target/products/org.openjdk.jmc/linux/gtk/x86_64/JDK\ Mission\ Control/jmc

# on aarch64
target/products/org.openjdk.jmc/linux/gtk/aarch64/JDK\ Mission\ Control/jmc
```

And here is an example for Windows x64:

```bash
"target\products\org.openjdk.jmc\win32\win32\x86_64\JDK Mission Control\jmc"
```

## Using the Built JMC Update Site in Eclipse
As part of the JMC build, the JMC update sites will be built. 

There is one update site for the stand-alone RCP application, providing plug-ins for the stand-alone release of JMC:

```bash
application/org.openjdk.jmc.updatesite.rcp/target/
```

There is another update site for the Eclipse plug-ins, providing plug-ins for running JMC inside of Eclipse:

```bash
application/org.openjdk.jmc.updatesite.ide/target/
```

To install it into Eclipe, simply open Eclipse and select Help | Install New Software... In the dialog, click Add... and then click the Archive... button. Select the built update site, e.g. 

```bash
application/org.openjdk.jmc.updatesite.ide/target/org.openjdk.jmc.updatesite.ide-9.1.0-SNAPSHOT.zip
```

## Setting up Development Environment
Please follow the [Developer Guide](docs/devguide/README.md).

## FAQ
For help with frequently asked questions, see the [JMC FAQ](https://wiki.openjdk.org/display/jmc/JMC+FAQ) on the JMC Wiki.

## License
The Mission Control source code is made available under the Universal Permissive License (UPL), Version 1.0 or a BSD-style license, alternatively. The full open source license text is available at license/LICENSE.txt in the JMC project.

## About
Mission Control is an open source project of the [OpenJDK](https://openjdk.org/).
The Mission Control project originated from the JRockit JVM project.
