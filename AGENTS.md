# JDK Mission Control (JMC) - Agent Instructions

## Project Overview
- Java Mission Control (JMC) is an OpenJDK project with core libraries and an Eclipse RCP UI.
- Main areas:
  - `core/` - Headless Java 17 libraries (can run standalone, read recordings from JDK 7+)
    - `org.openjdk.jmc.common` - Core APIs and utilities
    - `org.openjdk.jmc.flightrecorder` - JFR parsing and analysis
    - `org.openjdk.jmc.flightrecorder.rules` - Rule framework
    - `org.openjdk.jmc.flightrecorder.rules.jdk` - JDK-specific analysis rules
    - `org.openjdk.jmc.testlib` - Test utilities
  - `application/` - Eclipse RCP UI components (Java 21)
  - `agent/` - JFR Agent for bytecode instrumentation

## Build & Test
- `./build.sh --installCore` - Install core libraries
- `./build.sh --packageJmc` - Package full JMC application
- `./build.sh --test` - Run standard tests
- `./build.sh --testUi` - Run UI tests
- `mvn verify -Dtest.includes=**/*TestName*` - Run specific tests
- `mvn verify -Dspotbugs.skip=true` - Skip SpotBugs during verification

### Build Scripts
- `scripts/runcoretests.sh` - Run core library tests
- `scripts/runapptests.sh` - Run application tests
- `scripts/runagenttests.sh` - Run agent tests
- `scripts/startp2.sh` - Start local P2 repository for application build

### Eclipse Platform Profiles
Build against specific Eclipse platform versions using Maven profiles:
```bash
mvn verify -P 2024-12
```

## Java Version & Toolchains
- **Java Version**: JDK 21 for application build, JDK 17 for core components
- Requires both JDK 17 and JDK 21 configured in `~/.m2/toolchains.xml`.

Example `~/.m2/toolchains.xml`:
```xml
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <id>JavaSE-17</id>
      <version>17</version>
    </provides>
    <configuration>
      <jdkHome>/path/to/jdk17</jdkHome>
    </configuration>
  </toolchain>
  <toolchain>
    <type>jdk</type>
    <provides>
      <id>JavaSE-21</id>
      <version>21</version>
    </provides>
    <configuration>
      <jdkHome>/path/to/jdk21</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

## Code Style & Formatting
- Java formatting follows the Eclipse profile in `configuration/ide/eclipse/formatting/formatting.xml`.
- Java cleanup rules are in `configuration/ide/eclipse/formatting/clean-up-with-formatting.xml`.
- JavaScript formatting follows `configuration/ide/eclipse/formatting/formattingjs.xml`.
- Key formatter settings (from the Eclipse profiles):
  - Tabs for indentation (`tabulation.char=tab`, size 4).
  - Line length 120 (`lineSplit=120`).
  - Javadoc/comment line length 100 (`comment.line_length=100`).
- Avoid star imports; remove unused imports.
- **Naming**: Follow Eclipse/Java standard conventions, be consistent.

## Logging / Error Handling
- Use `Level.FINE` for expected exceptions (e.g., `CancellationException`).
- Use `Level.SEVERE` for unexpected failures.
- Filter cancellations from error logs during model rebuilding.
- Check SpotBugs exceptions for guidance.

## Commit Messages
- Format: `JIRA_NUMBER: Commit message` (example: `6789: Fix bug`).
- Issue tracker: https://bugs.openjdk.org/projects/JMC/issues

## Copyright Headers
- All modified files must have the current year in the copyright header.
- CI validates this via `scripts/checkcopyrightyear.sh`.
- Affected file types: `*.java`, `*.htm`, `pom.xml`, `*.properties`.

## Static Analysis
- **Checkstyle**: Enforces no star imports, no redundant/unused imports (`configuration/checkstyle/checkstyle.xml`).
- **SpotBugs**: Static bug detection with exclusions in `configuration/spotbugs/spotbugs-exclude.xml`.
- **Spotless**: Code formatting enforced during Maven validate phase.

## Internationalization (i18n)
- Core modules: Place `messages.properties`, `messages_ja.properties`, `messages_zh.properties` in `internal` packages.
- Application modules: Use separate l10n plugin modules (e.g., `org.openjdk.jmc.*.ja`, `org.openjdk.jmc.*.zh_CN`).
- Access strings via `Messages.getString(Messages.MESSAGE_KEY)`.

## Writing Flight Recorder Rules
Rules analyze JFR recordings and provide recommendations to users.

### Creating a New Rule
1. Extend `AbstractRule`:
```java
public class MyRule extends AbstractRule {
    public MyRule() {
        super("MyRuleId", Messages.getString(Messages.MY_RULE_NAME),
              JfrRuleTopics.TOPIC_NAME, CONFIGURATION_ATTRIBUTES,
              RESULT_ATTRIBUTES, Collections.emptyMap());
    }

    @Override
    protected IResult getResult(IItemCollection items, IPreferenceValueProvider vp,
                                IResultValueProvider rp) {
        // Analyze items using ItemFilters, Aggregators, etc.
        return ResultBuilder.createFor(this, vp)
            .setSeverity(Severity.get(score))
            .setSummary("Summary message")
            .setExplanation("Detailed explanation")
            .build();
    }
}
```

2. Register the rule in `META-INF/services/org.openjdk.jmc.flightrecorder.rules.IRule`:
```
org.openjdk.jmc.flightrecorder.rules.jdk.mypackage.MyRule
```

### Core Item Processing APIs
- `IItemCollection` - Collection of JFR events to query
- `IItemFilter` - Filter events (use `ItemFilters` factory)
- `IAggregator` - Aggregate values (use `Aggregators` factory)
- `IAttribute` - Access event attributes (see `JdkAttributes`)
- `RulesToolkit` - Utility methods for rule implementations

## Testing
- Unit tests in `src/test/java` using JUnit 4.
- Test resources in `src/test/resources`.
- JDP multicast tests are automatically skipped on macOS.
