# JFR Writer Performance Benchmarks

This module contains JMH (Java Microbenchmark Harness) benchmarks for measuring the performance of the JFR Writer API.

## Building

Build the benchmark JAR from the benchmark module directory:

```bash
cd tests/org.openjdk.jmc.flightrecorder.writer.benchmarks
mvn clean package
```

Or from the core root directory:

```bash
mvn clean package -DskipTests -f tests/org.openjdk.jmc.flightrecorder.writer.benchmarks/pom.xml
```

This creates an executable uber-JAR: `target/benchmarks.jar` (~4.7MB)

## Running Benchmarks

### List Available Benchmarks

```bash
java -jar target/benchmarks.jar -l
```

### Run All Benchmarks

Warning: This will take several hours as each benchmark runs multiple iterations with warmup.

```bash
java -jar target/benchmarks.jar
```

### Run Specific Benchmark Class

```bash
java -jar target/benchmarks.jar EventWriteThroughputBenchmark
```

### Run Single Benchmark Method

```bash
java -jar target/benchmarks.jar EventWriteThroughputBenchmark.writeSimpleEvent
```

### Quick Test Run (1 iteration, no warmup, 1 fork)

For quick verification or testing:

```bash
java -jar target/benchmarks.jar EventWriteThroughputBenchmark.writeSimpleEvent -i 1 -wi 0 -f 1
```

### Pattern Matching

Use regex patterns to run related benchmarks:

```bash
# Run all throughput benchmarks
java -jar target/benchmarks.jar ".*Throughput.*"

# Run all string encoding benchmarks
java -jar target/benchmarks.jar ".*StringEncoding.*"

# Exclude benchmarks
java -jar target/benchmarks.jar -e ".*Allocation.*"
```

## Profiling

### List Available Profilers

```bash
java -jar target/benchmarks.jar -lprof
```

### Run with GC Profiler

Measure allocation rates and GC pressure:

```bash
java -jar target/benchmarks.jar AllocationRateBenchmark -prof gc
```

### Run with Stack Profiler

Identify hotspots:

```bash
java -jar target/benchmarks.jar EventWriteThroughputBenchmark -prof stack
```

### Run with Async Profiler (if available)

Requires async-profiler to be installed:

```bash
java -jar target/benchmarks.jar EventWriteThroughputBenchmark -prof async:libPath=/path/to/libasyncProfiler.so
```

## Output Formats

### JSON Output

```bash
java -jar target/benchmarks.jar EventWriteThroughputBenchmark -rf json -rff results.json
```

### CSV Output

```bash
java -jar target/benchmarks.jar EventWriteThroughputBenchmark -rf csv -rff results.csv
```

### Multiple Formats

```bash
java -jar target/benchmarks.jar EventWriteThroughputBenchmark -rf json -rff results.json -rf text -rff results.txt
```

## Common Options

Run `java -jar target/benchmarks.jar -h` for all options. Most commonly used:

| Option | Description | Example |
|--------|-------------|---------|
| `-i N` | Number of measurement iterations | `-i 5` |
| `-wi N` | Number of warmup iterations | `-wi 3` |
| `-f N` | Number of forks | `-f 3` |
| `-t N` | Number of threads | `-t 4` |
| `-w TIME` | Warmup time per iteration | `-w 10s` |
| `-r TIME` | Measurement time per iteration | `-r 10s` |
| `-prof PROF` | Enable profiler | `-prof gc` |
| `-rf FORMAT` | Result format (json/csv/text) | `-rf json` |
| `-rff FILE` | Result output file | `-rff results.json` |
| `-p PARAM=V` | Override parameter value | `-p poolSize=1000` |

## Available Benchmarks

### EventWriteThroughputBenchmark

Measures event write throughput (operations per second) for different event types:

- `writeSimpleEvent` - Event with single long field
- `writeMultiFieldEvent` - Event with 5 mixed-type fields (long, int, double, boolean, String)
- `writeStringHeavyEvent` - Event with 4 string fields
- `writeRepeatedStringsEvent` - Event with repeated strings (tests string pool caching)

**Example:**
```bash
java -jar target/benchmarks.jar EventWriteThroughputBenchmark
```

### AllocationRateBenchmark

Measures allocation rates during event writing. Designed to be used with GC profiler:

- `measureEventWriteAllocations` - Single event write allocation
- `measureBatchEventWriteAllocations` - Batch of 100 events allocation

**Example:**
```bash
java -jar target/benchmarks.jar AllocationRateBenchmark -prof gc
```

### StringEncodingBenchmark

Isolates UTF-8 string encoding performance:

- `encodeRepeatedStrings` - Same strings each iteration (cache-friendly)
- `encodeUniqueStrings` - Unique strings with counter (uncached)
- `encodeMixedStrings` - Mix of cached and unique strings (realistic)
- `encodeUtf8Strings` - Multi-byte UTF-8 characters (CJK, Cyrillic, Arabic, emoji)

**Example:**
```bash
java -jar target/benchmarks.jar StringEncodingBenchmark
```

### ConstantPoolBenchmark

Tests constant pool HashMap performance with parameterized pool sizes:

- `buildConstantPoolWithUniqueStrings` - Tests HashMap growth and rehashing
- `buildConstantPoolWithRepeatedStrings` - Tests lookup performance
- `buildConstantPoolMixed` - Realistic mix pattern (70% cached, 30% unique)

**Parameters:** `poolSize=100,500,1000` (default: 100)

**Example:**
```bash
# Run with all parameter combinations
java -jar target/benchmarks.jar ConstantPoolBenchmark

# Run with specific pool size
java -jar target/benchmarks.jar ConstantPoolBenchmark -p poolSize=1000
```

## Comparing Results

Use the included `compare.py` Python script to compare two benchmark runs and see performance differences:

```bash
# Run baseline
java -jar target/benchmarks.jar EventWriteThroughputBenchmark -rf json -rff baseline.json

# Run after changes
java -jar target/benchmarks.jar EventWriteThroughputBenchmark -rf json -rff optimized.json

# Compare with custom title
python3 compare.py baseline.json optimized.json "My Optimization"
```

**Example Output:**
```
================================================================================
My Optimization
================================================================================

writeSimpleEvent
  Baseline:       943526.800 ops/s
  Optimized:      984670.381 ops/s
  Change:    ↑   4.36%

writeMultiFieldEvent
  Baseline:       787089.123 ops/s
  Optimized:      880622.456 ops/s
  Change:    ↑  11.88%
```

The script automatically detects benchmark mode and calculates improvements correctly:
- **Throughput modes** (ops/s): Higher is better, shows ↑ for improvements
- **Average time modes** (ms/op): Lower is better, shows ↓ for improvements

**Usage:**
```bash
python3 compare.py <baseline.json> <optimized.json> [optional_title]
```

## Configuration

Benchmarks use the following JVM settings by default (configured in `@Fork` annotations):

- Heap: `-Xms2G -Xmx2G`
- Threads: 1 (single-threaded by default)
- Forks: 2 (for statistical reliability)

Override these with command-line options:

```bash
# Custom heap size
java -Xms4G -Xmx4G -jar target/benchmarks.jar EventWriteThroughputBenchmark

# Or via JMH options
java -jar target/benchmarks.jar EventWriteThroughputBenchmark -jvmArgs "-Xms4G -Xmx4G"
```

## Interpreting Results

JMH reports several metrics:

- **Score**: Mean performance (ops/s for throughput, ms/op for average time)
- **Error**: Margin of error (99.9% confidence interval)
- **Units**: ops/s (operations per second), ms/op (milliseconds per operation), etc.

Higher ops/s = better performance
Lower ms/op = better performance

Always:
1. Run with multiple forks (`-f 3`) for statistical reliability
2. Ensure adequate warmup iterations (`-wi 5`)
3. Use profilers to understand *why* performance changes
4. Compare against baselines, not absolute numbers
5. Be aware of JVM optimizations (see JMH warnings about Blackholes)

## Troubleshooting

### Build Fails

If `mvn clean package` fails with MANIFEST.MF errors, ensure you're using the latest pom.xml which correctly configures the maven-jar-plugin to read the manifest from resources.

### Benchmark Hangs

Some benchmarks create temporary JFR files. If interrupted, clean up:

```bash
rm -rf /tmp/jfr-writer-mmap-*
```

### Out of Memory

Increase heap size:

```bash
java -Xms4G -Xmx4G -jar target/benchmarks.jar ...
```

### Inconsistent Results

- Ensure stable system load (close other applications)
- Increase forks: `-f 5`
- Increase iterations: `-i 10 -wi 5`
- Disable dynamic frequency scaling if possible

## References

- [JMH Documentation](https://github.com/openjdk/jmh)
- [JMH Samples](https://github.com/openjdk/jmh/tree/master/jmh-samples/src/main/java/org/openjdk/jmh/samples)
- [JMH Visualizer](https://jmh.morethan.io/) - Upload JSON results for visualization
