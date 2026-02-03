# Memory-Mapped File Implementation for JFR Writer

## Overview

This document describes the memory-mapped file (mmap) implementation for the JFR Writer, which provides bounded memory usage during recording creation.

## Architecture

### Components

1. **LEB128MappedWriter** - Fixed-size memory-mapped file writer
   - Location: `org.openjdk.jmc.flightrecorder.writer.LEB128MappedWriter`
   - Fixed capacity (no dynamic remapping)
   - Extends `AbstractLEB128Writer` for LEB128 encoding support
   - Key methods:
     - `canFit(int bytes)` - Check available space
     - `reset()` - Reset for buffer reuse
     - `force()` - Flush to disk
     - `copyTo(OutputStream)` - Export data

2. **ThreadMmapManager** - Per-thread double-buffer management
   - Location: `org.openjdk.jmc.flightrecorder.writer.ThreadMmapManager`
   - Manages two buffers per thread (active/inactive)
   - Background flushing via ExecutorService
   - File naming:
     - Buffers: `thread-{threadId}-buffer-{0|1}.mmap` (reused)
     - Flushed chunks: `chunk-{threadId}-{sequence}.dat` (persistent)

3. **Chunk** - Event serialization with automatic rotation
   - Modified to accept `LEB128Writer` and `ThreadMmapManager`
   - Checks `canFit()` before each event write
   - Triggers rotation when buffer full
   - Backward compatible with heap-based mode

4. **RecordingImpl** - Dual-mode recording implementation
   - Supports both mmap and legacy heap modes
   - Sequential finalization: header → chunks → checkpoint → metadata
   - Proper cleanup of temporary files

### Configuration

Enable mmap mode via the builder pattern API:

```java
// Default 4MB chunk size
Recording recording = Recordings.newRecording(outputStream,
    settings -> settings.withMmap()
                        .withJdkTypeInitialization());

// Custom chunk size
Recording recording = Recordings.newRecording(outputStream,
    settings -> settings.withMmap(8 * 1024 * 1024)  // 8MB per thread
                        .withJdkTypeInitialization());
```

**Configuration options:**
- `withMmap()` - Enable mmap with default 4MB chunk size
- `withMmap(int chunkSize)` - Enable mmap with custom chunk size in bytes
- Default behavior: Heap mode (backward compatible)

### Memory Usage

**Mmap mode:**
- Per-thread memory: 2 × chunk size (default 8MB per thread)
- Metadata/checkpoint: ~6MB heap (bounded)
- Total heap: ~6MB + O(threads)

**Legacy heap mode:**
- All event data in heap
- Unbounded growth with event count

## Implementation Details

### Buffer Rotation Flow

1. Thread writes events to active mmap buffer
2. Before each write, `canFit()` checks available space
3. When full:
   - Swap active ↔ inactive buffers
   - Submit inactive buffer for background flush
   - Continue writing to new active buffer
4. Background thread:
   - Flushes buffer to chunk file
   - Resets buffer for reuse

### Finalization Flow

**Mmap mode:**
1. Call `mmapManager.finalFlush()` - flush all active buffers
2. Collect all flushed chunk files
3. Calculate offsets:
   - Checkpoint offset = header size (68 bytes) + total chunks size
   - Metadata offset = checkpoint offset + checkpoint event size
4. Write sequentially:
   - Header with correct offsets
   - All chunk files (via Files.copy)
   - Checkpoint event
   - Metadata event
5. Cleanup temp files

**Legacy heap mode:**
- Unchanged - writes globalWriter.export() with in-place offset patching

## Testing

### Unit Tests

**LEB128MappedWriter** (22 tests)
- Basic write operations
- LEB128 encoding
- Buffer capacity checking
- Reset and reuse
- Force and copyTo
- Large writes
- Edge cases

**ThreadMmapManager** (13 tests)
- Active writer creation
- Multiple threads
- Buffer rotation
- Background flushing
- Final flush
- Cleanup
- Concurrent access

### Integration Tests

**MmapRecordingIntegrationTest** (5 tests)
- Basic recording (100 events)
- Multi-threaded recording (4 threads, 1000 events)
- Large events triggering rotation (>512KB)
- File output verification
- Mmap vs heap comparison (same size ±10%)

All tests pass successfully.

## Performance Benchmarking

### Existing Benchmarks

JMH benchmarks exist in `tests/org.openjdk.jmc.flightrecorder.writer.benchmarks`:

- `EventWriteThroughputBenchmark` - Events per second for various event types
- `AllocationRateBenchmark` - Allocation rates during event writing
- `ConstantPoolBenchmark` - Constant pool performance
- `StringEncodingBenchmark` - String encoding performance

### Running Benchmarks

To compare mmap vs heap performance:

```bash
# Build benchmark uberjar
cd tests/org.openjdk.jmc.flightrecorder.writer.benchmarks
mvn clean package

# Run all throughput benchmarks
java -jar target/benchmarks.jar EventWriteThroughputBenchmark

# Run specific benchmark
java -jar target/benchmarks.jar EventWriteThroughputBenchmark.writeSimpleEvent

# Save results for comparison
java -jar target/benchmarks.jar EventWriteThroughputBenchmark -rf json -rff results.json

# Compare two runs
python3 compare.py baseline.json optimized.json "Mmap vs Heap"
```

See `tests/org.openjdk.jmc.flightrecorder.writer.benchmarks/README.md` for detailed benchmark documentation.

### Performance Goals

- **Throughput**: < 5% regression vs heap mode
- **Memory**: Bounded at ~6MB + (threads × 8MB)
- **Latency**: No significant increase in p99 event write time

## Backward Compatibility

The implementation maintains full backward compatibility:

- **Default behavior unchanged**: Mmap mode is opt-in via builder pattern API
- **Public API additions**: New `withMmap()` methods in `RecordingSettingsBuilder` (marked `@since 10.0.0`)
- **No breaking changes**: Existing API unchanged, all constructors preserved
- **File format unchanged**: Generated JFR files identical to legacy mode
- **All existing tests pass**: No regressions in functionality

The legacy heap-based code path (`chunkDataQueue`, `chunkDataMergingService`) is preserved and used when mmap is not explicitly enabled.

## Future Improvements

Potential optimizations (not implemented):

1. **Adaptive chunk sizing**: Adjust chunk size based on workload
2. **Zero-copy finalization**: Direct file concatenation without intermediate copies
3. **Compression**: On-the-fly compression of flushed chunks
4. **Lock-free rotation**: Further reduce contention during buffer swaps

## References

- JFR File Format: [JEP 328](https://openjdk.org/jeps/328)
- LEB128 Encoding: [Wikipedia](https://en.wikipedia.org/wiki/LEB128)
- Memory-Mapped Files: `java.nio.MappedByteBuffer`
