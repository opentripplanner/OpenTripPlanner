# Shape service

Owns the geometry of trip patterns. The geometry of a pattern is a `CompactLineStringSequence`: one
`CompactLineString` per hop (stop to stop) plus a table of cumulative distances.

The module follows the same design as the timetable data (`TimetableDataImport` →
`ScheduledTimetableData` → `TimetableRepository` publishing snapshots):

- `ShapeService` – read access, used by routing, itinerary mapping and the GraphQL APIs. It is
  request-scoped and backed by a `ShapeRepositorySnapshot`. Asking for the geometry of an unknown
  pattern is a bug and fails with `IllegalStateException`.
- `ShapeDataImport` (`dataimport`) – mutable, filled by the GTFS and NeTEx graph builders and
  serialized with the graph. `toScheduledShapeData()` freezes it into the immutable
  `ScheduledShapeData`.
- `ShapeRepository` – the long-lived, transactional buffer. It owns the `ScheduledShapeData` and the
  geometries of patterns created by real-time updates, and publishes an immutable
  `ShapeRepositorySnapshot` at commit. A snapshot reads real-time geometries first and falls back to
  the scheduled ones.

## Data flow

1. **Graph build:** the GTFS/NeTEx mappers create geometries with `TripGeometryFactory`, and
   `AddTransitEntitiesToTimetable` stores them in the `ShapeDataImport`. Patterns without shape data
   get straight lines between the stops.
2. **Startup:** the `ShapeDataImport` is converted to `ScheduledShapeData` and handed to the
   `ShapeRepository` (see `ShapeModule`).
3. **Real-time:** `RealTimeShapeResolver` (`updater/trip/patterncache`) writes the geometry of every
   new real-time pattern when the update is applied, derived from the pattern it replaces.
4. **Requests:** each request gets a `ShapeService` over the snapshot of its transaction scope.

Identical hops are deduplicated, so equal hops share one instance (Kryo reference tracking keeps
this across serialization).

## Glossary

| Term                        | Meaning                                                                                   |
| --------------------------- | ----------------------------------------------------------------------------------------- |
| Hop                         | The segment between two consecutive stops of a pattern.                                   |
| Trip pattern geometry       | All hops of a pattern, as a `CompactLineStringSequence`.                                  |
| `ShapeService`              | Read-only, request-scoped lookup of a pattern's geometry.                                 |
| `ShapeRepository`           | Mutable buffer for the writer thread; publishes snapshots.                                |
| `ShapeRepositorySnapshot`   | Immutable, read-only view of committed shapes.                                            |
| `ShapeRepositoryLifecycle`  | Plugs the repository into the transaction framework (buffer → snapshot at commit).        |
| `ScheduledShapeData`        | Immutable geometries of the scheduled patterns, loaded from the graph.                    |
| `ShapeDataImport`           | Mutable graph-build counterpart of `ScheduledShapeData`; serialized with the graph.       |
| `CompactLineStringSequence` | Immutable list of hops plus cumulative distances; decodes hops on demand.                 |
| `CompactLineString`         | One hop in packed form (package-private); equal hops are shared.                          |
| `PackedLineStringCodec`     | Encodes/decodes the packed byte form; lives in the `street` module.                       |
| `TripGeometryFactory`       | Builds a `CompactLineStringSequence` from line strings or from straight lines.            |
| `RealTimeShapeResolver`     | Creates the geometry for real-time patterns (copied from the original or straight lines). |
