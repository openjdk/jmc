# JFR Writer Performance Benchmarks

## Overview

This document describes the JMH (Java Microbenchmark Harness) benchmarks created for the JFR Writer module to measure and track performance improvements, particularly focusing on allocation reduction and throughput optimization.

## Benchmark Module Location

```
core/tests/org.openjdk.jmc.flightrecorder.writer.benchmarks/
```

## Building the Benchmarks

### Prerequisites
- Maven 3.6+
- JDK 17+

### Build Steps

1. **Build the entire core module** (required to install dependencies):
   ```bash
   cd core
   mvn clean install -DskipTests
   ```

2. **Build the benchmark module**:
   ```bash
   cd tests/org.openjdk.jmc.flightrecorder.writer.benchmarks
   mvn clean package -DskipTests
   ```

3. **Verify the benchmark JAR was created**:
   ```bash
   ls -lh target/benchmarks.jar
   ```
   Expected: ~4.7MB executable JAR

## Running Benchmarks

### Quick Test Run (Fast, for validation)
```bash
cd core/tests/org.openjdk.jmc.flightrecorder.writer.benchmarks
java -jar target/benchmarks.jar -wi 1 -i 2 -f 1
```
- `-wi 1`: 1 warmup iteration
- `-i 2`: 2 measurement iterations
- `-f 1`: 1 fork

### Full Baseline Run (Recommended for accurate measurements)
```bash
java -jar target/benchmarks.jar -rf json -rff baseline.json
```
- Uses default: 3 warmup iterations, 5 measurement iterations
- Outputs JSON results to `baseline.json`

### Run Specific Benchmark
```bash
# Event throughput benchmarks
java -jar target/benchmarks.jar EventWriteThroughput

# Allocation benchmarks (with GC profiler)
java -jar target/benchmarks.jar AllocationRate -prof gc

# String encoding benchmarks
java -jar target/benchmarks.jar StringEncoding

# Constant pool benchmarks
java -jar target/benchmarks.jar ConstantPool
```

### Advanced Options

#### With Allocation Profiling
```bash
java -jar target/benchmarks.jar -prof gc
```

#### With JFR Profiling
```bash
java -jar target/benchmarks.jar -prof jfr
```

#### List All Benchmarks
```bash
java -jar target/benchmarks.jar -l
```

#### Get Help
```bash
java -jar target/benchmarks.jar -h
```

## Benchmark Descriptions

### 1. EventWriteThroughputBenchmark
**Purpose**: Measures events/second for different event types.

**Benchmarks**:
- `writeSimpleEvent`: Single long field event
- `writeMultiFieldEvent`: Event with 5 fields (long, string, int, double, boolean)
- `writeStringHeavyEvent`: Event with 4 unique string fields
- `writeRepeatedStringsEvent`: Event with repeated strings (tests caching)

**Metric**: Throughput (ops/sec) - Higher is better

**Use Case**: Validates improvements from LEB128Writer pooling and field value caching

### 2. AllocationRateBenchmark
**Purpose**: Measures allocation rate (MB/sec) during event writing.

**Benchmarks**:
- `measureEventWriteAllocations`: Single event write
- `measureBatchEventWriteAllocations`: Batch of 100 events

**Metric**: Allocation rate (MB/sec) - Lower is better

**Use Case**: Primary metric for allocation reduction optimizations

**Recommended Run**:
```bash
java -jar target/benchmarks.jar AllocationRate -prof gc
```

### 3. StringEncodingBenchmark
**Purpose**: Measures UTF-8 encoding performance.

**Benchmarks**:
- `encodeRepeatedStrings`: Same strings repeatedly (tests cache hits)
- `encodeUniqueStrings`: Unique strings each time (tests cache misses)
- `encodeMixedStrings`: Mix of cached and uncached
- `encodeUtf8Strings`: Multi-byte UTF-8 characters

**Metric**: Throughput (ops/sec) - Higher is better

**Use Case**: Validates UTF-8 caching effectiveness

### 4. ConstantPoolBenchmark
**Purpose**: Measures constant pool buildup and lookup performance.

**Benchmarks**:
- `buildConstantPoolWithUniqueStrings`: Tests HashMap growth (100/500/1000 events)
- `buildConstantPoolWithRepeatedStrings`: Tests deduplication
- `buildConstantPoolMixed`: Mix of unique and repeated

**Metric**: Average time (ms) - Lower is better

**Use Case**: Validates HashMap initial capacity optimization

## Baseline Results

### Test Environment
- **Date**: 2025-12-03
- **JDK**: OpenJDK 21.0.5+11-LTS
- **OS**: macOS 14.6 (Darwin 24.6.0)
- **CPU**: Apple M1 Max
- **Heap**: 2GB (-Xms2G -Xmx2G)
- **Branch**: jb/JMC-7992 (pre-optimization)

### Results Summary

**Note**: This is a quick baseline run using `-wi 1 -i 2 -f 1` for faster validation. For production benchmarking, use the full configuration with 3 warmup iterations and 5 measurement iterations.

#### Event Throughput Benchmarks (ops/sec - Higher is Better)
```
Benchmark                                               Mode  Cnt       Score  Units
EventWriteThroughputBenchmark.writeSimpleEvent         thrpt    2  986,095.2  ops/s
EventWriteThroughputBenchmark.writeMultiFieldEvent     thrpt    2  862,335.4  ops/s
EventWriteThroughputBenchmark.writeStringHeavyEvent    thrpt    2  866,022.4  ops/s
EventWriteThroughputBenchmark.writeRepeatedStringsEvent thrpt    2  861,751.4  ops/s
```

**Key Insights**:
- Simple events (single long field): ~986K ops/s
- Multi-field events (5 fields): ~862K ops/s (12.5% slower)
- String-heavy events show similar performance, indicating effective constant pool deduplication
- Repeated strings perform nearly identically to unique strings in throughput

#### Allocation Benchmarks (ops/sec - Higher is Better)
```
Benchmark                                              Mode  Cnt       Score  Units
AllocationRateBenchmark.measureEventWriteAllocations  thrpt    2  899,178.8  ops/s
AllocationRateBenchmark.measureBatchEventWriteAllocations thrpt    2    7,197.8  ops/s
```

**Key Insights**:
- Single event write: ~899K ops/s
- Batch of 100 events: ~7.2K ops/s (equivalent to ~720K single events/s)
- Batch performance is 20% slower than single events, indicating per-batch overhead
- **Recommendation**: Run with `-prof gc` to measure actual allocation rates in MB/sec

#### String Encoding Benchmarks (ops/sec - Higher is Better)
```
Benchmark                                           Mode  Cnt       Score  Units
StringEncodingBenchmark.encodeUtf8Strings          thrpt    2  890,130.4  ops/s
StringEncodingBenchmark.encodeRepeatedStrings      thrpt    2  866,965.5  ops/s
StringEncodingBenchmark.encodeMixedStrings         thrpt    1  177,723.1  ops/s*
StringEncodingBenchmark.encodeUniqueStrings        thrpt    1   72,763.6  ops/s*
```

**Key Insights**:
- UTF-8 strings with multi-byte characters: ~890K ops/s (no performance penalty vs ASCII)
- Repeated strings (cache hits): ~867K ops/s
- Mixed strings: ~178K ops/s
- Unique strings: ~73K ops/s (11.9x slower than repeated strings)

**\*Warning**: Both `encodeMixedStrings` and `encodeUniqueStrings` encountered OutOfMemoryError during iteration 2 in the teardown phase. The constant pool grew too large during the 10-second measurement iterations (accumulating millions of unique strings). This indicates:
1. A potential performance issue with unbounded constant pool growth
2. The benchmarks may need to periodically close and reopen recordings to clear the constant pool
3. These numbers are based on only 1 iteration instead of 2

#### Constant Pool Benchmarks (ms/op - Lower is Better)
```
Benchmark                                                    (poolSize)  Mode  Cnt   Score  Units
ConstantPoolBenchmark.buildConstantPoolWithUniqueStrings          100  avgt    2   0.108  ms/op
ConstantPoolBenchmark.buildConstantPoolWithUniqueStrings          500  avgt    2   0.542  ms/op
ConstantPoolBenchmark.buildConstantPoolWithUniqueStrings         1000  avgt    2   1.021  ms/op

ConstantPoolBenchmark.buildConstantPoolWithRepeatedStrings        100  avgt    2   0.108  ms/op
ConstantPoolBenchmark.buildConstantPoolWithRepeatedStrings        500  avgt    2   0.527  ms/op
ConstantPoolBenchmark.buildConstantPoolWithRepeatedStrings       1000  avgt    2   1.060  ms/op

ConstantPoolBenchmark.buildConstantPoolMixed                      100  avgt    2   0.112  ms/op
ConstantPoolBenchmark.buildConstantPoolMixed                      500  avgt    2   0.563  ms/op
ConstantPoolBenchmark.buildConstantPoolMixed                     1000  avgt    2   1.105  ms/op
```

**Key Insights**:
- Constant pool buildup scales approximately linearly with pool size
- 100 events: ~0.11 ms
- 500 events: ~0.54 ms (5x)
- 1000 events: ~1.03 ms (9.4x)
- Repeated strings are slightly faster (2.7% at 500 events) but within measurement variance
- Mixed workload shows similar performance to unique strings

### Performance Bottlenecks Identified

1. **Unique String Handling**: 11.9x performance degradation when writing unique strings vs repeated strings
2. **Constant Pool Memory Growth**: OutOfMemoryError with 2GB heap during long-running string encoding benchmarks
3. **Batch Overhead**: 20% throughput reduction when writing events in batches
4. **Multi-field Events**: 12.5% slower than simple events

## Performance Optimization Plan

The benchmark results establish a baseline for the following planned optimizations:

### Phase 1: Critical Allocation Hotspots
1. **LEB128Writer pooling** (Chunk.java:155)
   - Expected: 70-80% allocation reduction
   - Benchmark: AllocationRateBenchmark, EventWriteThroughputBenchmark

2. **Field values caching** (TypedValueImpl.java:142)
   - Expected: 60% allocation reduction for multi-field events
   - Benchmark: EventWriteThroughputBenchmark.writeMultiFieldEvent

### Phase 2: String & Constant Pool
3. **UTF-8 encoding cache** (AbstractLEB128Writer.java)
   - Expected: 40% CPU reduction, 20% allocation reduction
   - Benchmark: StringEncodingBenchmark.encodeRepeatedStrings

4. **HashMap capacity hints** (ConstantPool.java)
   - Expected: 30% allocation reduction during pool buildup
   - Benchmark: ConstantPoolBenchmark

### Phase 3: CPU Optimizations
5. **Reflection caching** (RecordingImpl.java)
   - Expected: 50% startup improvement
   - Manual measurement required

6. **LEB128 encoding optimization**
   - Expected: 15% encoding CPU reduction
   - Benchmark: EventWriteThroughputBenchmark

## Comparing Results

### Before vs After
```bash
# Run baseline before optimizations
java -jar target/benchmarks.jar -rf json -rff baseline.json

# After implementing optimizations
java -jar target/benchmarks.jar -rf json -rff optimized.json

# Compare (using JMH compare tool or manual analysis)
```

### Expected Improvements
- **Allocation Rate**: 60-70% reduction
- **Event Throughput**: 40-50% increase
- **String Encoding (cached)**: 2-3x faster
- **Constant Pool Buildup**: 30% faster

## Troubleshooting

### Build Issues
- **Missing dependencies**: Run `mvn clean install -DskipTests` from `core/` directory first
- **Compilation errors**: Ensure JDK 17+ is being used

### Runtime Issues
- **OutOfMemoryError**: Increase heap size: `-Xms4G -Xmx4G`
- **Benchmarks take too long**: Use quick mode: `-wi 1 -i 1`

## References

- JMH Documentation: https://github.com/openjdk/jmh
- Performance Optimization Plan: `~/.claude/plans/snug-growing-horizon.md`
