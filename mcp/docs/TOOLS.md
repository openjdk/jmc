# Tool guide

[Back to the README](../README.md)

For the tool overview, recording visibility and stored result sets, see the [README](../README.md#tools).

## Suggested workflow

1. `loadRecording`, then `getRuleResults` to see what JMC already knows is wrong.
2. `getEventTypes` / `getSharedAttributes` to see what the recording contains and how it can be correlated.
3. `aggregateEvents` or `getTimeSeries` to find the specific bad instance or interval.
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
