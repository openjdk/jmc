# Mission Control

Mission Control is an open source production time profiling and diagnostics tool for Java.

Builds of Mission Control can currently be found in the Oracle JDK on supported platforms and in the Eclipse marketplace. 

For more information on Mission Control, see http://www.oracle.com/missioncontrol.

### Core API Features

* Core APIs for parsing and processing Java flight recordings 

* Core API can *read* recordings from JDK 7 and above

* Core API can *run* on JDK 7 and above

* Core API contains a framework for handling units of measurement and physical quantities

* Core API supports headless analysis of Java flight recordings


### Application Features

* An application supporting framework for hosting various useful Java tools 

* A tool for visualizing the contents of Java flight recordings, and the results of an automated analysis of the contents

* A JMX Console 

* A tool for heap waste analysis


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


Example for programmatically running rules in parallel (requires JDK8):

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
1. Install JDK 8, and make sure it is the JDK in use (java -version)

2. Install Maven (version 3.3.x. or above)

First get third party dependencies into a local p2 repo and make it available on localhost:

```bash
cd missioncontrolfolder [where you just cloned the sources]
cd releng/third-party
mvn p2:site
mvn jetty:run
```

Then in another terminal (in the project root):

```bash
cd core
mvn clean install
cd ..
mvn package
```
Note that you may need to define proxy settings if you happen to be behind a firewall. In your ~/.m2/settings.xml file (if you have none, simply create one), add:

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

> Currently, in order to run UI tests you need to supply the Jemmy UI testing libraries yourself. These can be built from source available at the mercurial repository at http://hg.openjdk.java.net/code-tools/jemmy/v3/.

>1. Create a directory on your local drive where you wish to build the Jemmy libraries.
>2. In a terminal, when in the newly created directory, issue `hg clone http://hg.openjdk.java.net/code-tools/jemmy/v3/`. If you don't have a Mercurial client you can download the code from http://hg.openjdk.java.net/code-tools/jemmy/v3/archive/tip.zip (or .gz or .bz2).
>3. Build Jemmy by issuing `mvn clean package`. Adding `-DskipTests` makes sure that UI tests that might fail won't stop the packaging.
>4. Copy the resulting jar files from core/JemmyCore/target, core/JemmyAWTInput/target, core/JemmyBrowser/target and SWT/JemmySWT/target to \[jmc_repo_dir\]/application/uitests/org.openjdk.jmc.test.jemmy/lib/ (create the lib directory first if it does not exist).

>(As soon as Jemmy is published on Maven Central, this manual build step will be removed.)

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

The above will not run StacktraceModelTest, as that is also matched by "test.excludes".

Note that if UI-tests are supposed to be part of the filtered run the "uitests" profile needs to be specified as well. Otherwise the UI won't start up and so the tests fail.

## Running the Locally Built JMC
The built JMC will end up in the `target` folder in the root. To run it, go to `target/products/org.openjdk.jmc/<platform>` to find the launcher. Don't forget to override the vm flag with the JVM you wish to use for running JMC.

Here is an example for Mac OS X:

```bash
target/products/org.openjdk.jmc/macosx/cocoa/x86_64/JDK\ Mission\ Control.app/Contents/MacOS/jmc -vm $JAVA_HOME/bin
```

Here is an example for Linux:

```bash
target/products/org.openjdk.jmc/linux/gtk/x86_64/jmc -vm $JAVA_HOME/bin
```

And here is an example for Windows x64:

```bash
target\products\org.openjdk.jmc\win32\win32\x86_64\jmc.exe -vm %JAVA_HOME%\bin
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
application/org.openjdk.jmc.updatesite.ide/target/org.openjdk.jmc.updatesite.ide-7.1.0-SNAPSHOT.zip
```

## Setting Up for Development and Launching in Eclipse
First make sure that you have a recent version of Eclipse. An Eclipse 2018-09 with the JDK 11 plug-in installed (available from Eclipse Marketplace) will do. You may also want to install the Mercurial Plug-in for Eclipse (MercurialEclipse). The Eclipse Marketplace is available under **Help | Eclipse Marketplace...**.

To set Eclipse up for JMC development, do the following:

1. First ensure that you have started the jetty server in the first step of building JMC.
2. Next open (File | Open...) the Eclipse target platform of interest, for example releng/platform-definitions/platform-definition-photon/platform.target
3. In the upper right corner of the platform editor that opens, click the link "Set as Active Target Platform"
4. Import the projects you are interested in (core and/or application) into a recent Eclipse.
5. If importing the application projects, make sure you create a user library (Preferences | Java/Build Path/User Libraries) named JMC_JDK, and add (Add External JARs...) the following JARs from a JDK 8 (u40 or above) to the User Library:
 - tools.jar (<JDK>/lib/tools.jar)
 - jconsole.jar (<JDK>/lib/jconsole.jar)
 - jfxswt.jar (<JDK>/jre/lib/jfxswt.jar)
 - jfxrt.jar (<JDK>/jre/lib/ext/jfxrt.jar)

Note that importing configuration/ide/eclipse as an Eclipse project should automatically make the development launchers available to you.

## License
The Mission Control source code is made available under the Universal Permissive License (UPL), Version 1.0 or a BSD-style license, alternatively. The full open source license text is available at license/LICENSE.txt in the JMC project.

## About
Mission Control is an open source project of the [OpenJDK](http://openjdk.java.net/).
The Mission Control project originated from the JRockit JVM project.