# Tool guide

[Back to the README](../README.md)

For the tool overview, recording visibility and stored result sets, see the [README](../README.md#tools).

## Suggested workflow

1. `loadRecording`, then `getRuleResults` to see what JMC already knows is wrong.
2. `getEventTypes` / `getSharedAttributes` to see what the recording contains and how it can be correlated.
3. `aggregateEvents` or `getTimeSeries` to find the specific bad instance or interval, and `getHistogram` to tell an
   outlier from a pattern.
4. `findRelatedEvents` with `storeAs`, then `combineResultSets`, to work out what coincided with it.
5. `getEventTable` or `getStackTrace` to read the detail.

## Compound filters and grouped statistics

Use `aggregateGroups` to answer grouped questions in one call. For example, substitute the event type and attribute
identifiers discovered with `getEventTypes` / `getAttributes`:

```json
{
  "eventType": "game.Terrain",
  "groupBy": "vehicle",
  "attributes": "duration,remainingSeconds",
  "filters": [
    {
      "attribute": "hpAtStart",
      "operator": "eq",
      "value": "0"
    },
    {
      "attribute": "iterations",
      "operator": "eq",
      "value": "6"
    },
    {
      "attribute": "remainingSeconds",
      "operator": "gt",
      "value": "0"
    }
  ],
  "percentiles": [
    50,
    90,
    95,
    99
  ],
  "storeAs": "terrain.filtered"
}
```

Each group includes its event count and statistics for each requested numeric attribute. Filters are AND-combined;
operators are `eq`, `ne`, `gt`, `gte`, `lt`, `lte`, `contains`, `exists` and `notExists`. Numeric operands are decimal
strings. A quantity operand uses the default unit of its quantity kind unless `unit` is given, for example
`{"attribute":"duration","operator":"gt","value":"1","unit":"ms"}`. Text equality is exact and `contains` matches a
substring. Missing and non-finite values fail every comparison, including `ne`; use `notExists` to select missing
values. `groupBy` takes comma-separated attributes; omit it for overall statistics.

Statistics use the first observed unit for each attribute within a group. Missing and non-finite values are excluded
and reported as `missingCount` and `nonFiniteCount`; `valueCount` is the number of values used. Standard deviation is
for the population. Percentiles, including the median, interpolate linearly between sorted values at `(n-1)*p/100`;
the defaults are 50, 90, 95 and 99. Statistics cover all matching events, even when only one page of groups is
returned.

`queryEvents` accepts the same filters and optional `resultSet`, returning precise rows with selected `columns`.
Both tools can store the complete filtered selection through `storeAs`, independently of their output limit.

To compare two recordings, load both and call `compareRecordings` with `baselineRecordingId` and
`candidateRecordingId`, plus the same `eventType`, `filters`, `groupBy`, `attributes` and `percentiles` shown above.
It returns the statistics for each side, candidate minus baseline deltas, and percentage changes. Groups found on
only one side are marked as such. Percentage changes from a zero or missing baseline are null. Time bounds are
relative to the start of each recording. When grouping by thread, threads are matched by name, since thread IDs
differ between runs. Counts and sums are not normalized, so compare recordings or time windows
of similar length.

## Distributions

`getHistogram` returns the exact distribution of one numeric attribute, `duration` by default, using the same
`eventType`, `filters`, `resultSet`, time bounds and `storeAs` as `aggregateGroups`. For example, to see whether
`java.util.concurrent` parking is dominated by a few long waits:

```json
{
  "eventType": "jdk.ThreadPark",
  "filters": [
    {
      "attribute": "duration",
      "operator": "gte",
      "value": "1",
      "unit": "ms"
    }
  ]
}
```

Buckets default to a log scale in a 1-2-5 sequence; set `bucketing` to `linear` and `buckets` to a count for equal
widths. Each bucket includes `from` and excludes `to`, except that the last bucket also includes `to`, and carries
`count`, `percent` and `cumulativePercent`. Only the range from the smallest to the largest value is covered,
but empty buckets in between are kept so gaps and multiple modes stay visible. Zero and negative values get a leading
bucket from the minimum. Durations are shown in milliseconds and memory sizes in bytes unless `unit` is given, for
example `us` or `KiB`. Values are plain numbers in that unit.

Each percentile carries `countAtOrAbove`, the number of values at or above it, so a high p99.9 backed by one or two
events can be told apart from a pattern. The defaults are 50, 90, 99, 99.9, 99.99 and 100. Percentiles are computed
exactly, with the same linear interpolation as `aggregateGroups`, so both tools agree on the same selection.

For durations, `recordingThresholds` lists the JFR threshold of each event type in the selection. Events shorter than
the threshold were never recorded, so the low end of the distribution is cut off there; it is not evidence that short
events did not happen. When the threshold changed during the recording, the highest value is reported.

### Selecting by percentile

With `storeFromPercentile` and/or `storeToPercentile`, `storeAs` keeps only the events whose value lies in that
percentile range, rather than every filtered event. The range includes `from` and excludes `to`, except that a `to` of
100 includes the maximum, so adjacent ranges never overlap. The bounds are the same interpolated values as the
reported percentiles, so `storedEventCount` for `storeFromPercentile: 99` equals the `countAtOrAbove` of p99.
`storedRange` reports the percentiles and the values they resolved to. Events without a value for the attribute are
never stored.

The stored set is an ordinary result set, so it composes with every other tool. For example, with the
`jdk.JavaMonitorEnter` events already stored as `blocking`, the slowest percent of them can be stored and then
aggregated:

```json
{
  "resultSet": "blocking",
  "percentiles": [99],
  "storeAs": "slowBlocking",
  "storeFromPercentile": 99
}
```

`aggregateGroups` with `resultSet` set to `slowBlocking` then gives their average duration, `getStackTrace` shows where
they were blocked, and `getHistogram` can split them further. To select a value range rather than a percentile range,
use `filters` with `gte` and `lt` instead.

## Precise results and export

`queryEvents`, `aggregateGroups`, `compareRecordings`, `getStackStatistics`, `joinIntervals` and `exportResultSet`
return MCP structured content. Numeric values include `value`, a lossless decimal `exactValue` string, a
locale-independent `unit`, and `quantityKind`. Values with a unit also include `canonicalValue`,
`canonicalExactValue` and `canonicalUnit`, expressed in the default unit of the quantity kind, so units such as ticks
can be read as seconds. Threads include `id` and `name`. Clients that parse JSON numbers as doubles should use the
exact value strings for large integers. Non-finite values have a null `value` and an `exactValue` such as `NaN` or
`Infinity`.

To export the stored selection above, call:

```json
{
  "resultSet": "terrain.filtered",
  "columns": "startTime,duration,vehicle,hpAtStart,remainingSeconds",
  "format": "json",
  "offset": 0,
  "limit": 1000
}
```

Call `exportResultSet` again with the returned `nextOffset` until it is null; `total`, `returned` and `truncated`
describe the page. `format: "csv"` returns CSV text with quantities and threads encoded as JSON cells. Rows are in
the order of the stored result set, grouped by event type, not sorted by time. The data is returned to the client;
nothing is written on the server.

## Scoped stacks and interval joins

`getStackTrace` and `getStackStatistics` accept `sourceResultSet`, `thread` (exact name or Java thread ID), and
`frameContains` (substring of a fully qualified method). With the default `denominator: "filtered"`, percentages are
relative to the selected events that have stacks and positive weights, so a query for one thread reports shares of
that thread's samples. With `denominator: "source"`, percentages are relative to the selection by event type, time
and result set, before thread and frame filtering. The result reports the denominator and the number of events with
missing stacks and missing, zero or invalid weights. Each row is a call path with self and inclusive weights and
percentages. Inclusive weights of different rows overlap and must not be summed. Count weights are counts, duration
weights are seconds and memory weights are KiB; other weights use the default unit of their quantity kind. The
`*ExactValue` strings carry every digit for clients that parse numbers as doubles.

To attach both an enclosing physics phase and tick to every selected terrain event:

```json
{
  "childResultSet": "terrain.filtered",
  "parentEventType": "game.PhysicsPhase,game.SimulationTick",
  "matchAttributes": "run",
  "sameThread": true,
  "childColumns": "vehicle,hpAtStart,duration",
  "parentColumns": "phase,tickId,run"
}
```

`joinIntervals` checks containment for each child separately, with inclusive start and end boundaries. By default the
thread must match, and keys listed in `matchAttributes`, such as `run`, must match too; a missing key never matches.
For each parent type it picks the shortest enclosing interval, then the earliest start, then source order. Each row
includes the chosen parents and the number of candidates. Children without a parent are kept by default. The summary
covers the whole join: `matchedChildren` have at least one parent, `fullyMatchedChildren` have one of every requested
type, and `unmatchedChildren` have none. Children and parents can be given as event types or stored result sets.

## Notes and limitations

- **Recordings are held in memory.** `loadRecording` parses the whole file, and it stays resident until
  `unloadRecording`. A multi-gigabyte recording needs a correspondingly large heap.
- **Rule results are cached** per recording, and computed on the first `getRuleResults` call. On a large recording that
  first call can take a while.
- **Result sets are per recording** and compared by event identity, so `combineResultSets` only makes sense for sets
  derived from the same loaded recording.
- **Event data is untrusted.** Thread names, class names, stack frames and log messages in a recording come from the
  profiled application. The tool descriptions tell the model not to follow instructions found in event data, but treat
  anything surfaced from a recording as data.
- **Output is capped.** Every listing tool has a default and a hard cap on how much it returns, and says so when it
  truncates. Cross-type queries lead with an event type breakdown so a truncated table never hides a whole event type.
  The structured query, grouping and export tools are paged, and `storeAs` always keeps the complete selection.
  Percentiles hold and sort every value of a group in memory, so large selections need more heap.
