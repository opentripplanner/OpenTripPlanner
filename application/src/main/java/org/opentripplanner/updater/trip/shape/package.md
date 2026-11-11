# Real-time shapes

Creates and stores the geometry of trip patterns created by real-time updates. The geometries live
in the `ShapeRepository` (see the [shape service](../../../service/shape/package.md)).

- `RealTimeTripShapeFactory` – creates a geometry. It has no repository access: the geometry of the
  original pattern is given as input, and without it straight lines between the stops are used.
- `RealTimeShapeWriter` – decides when a geometry is needed (new real-time patterns without one) and
  stores the result in the repository. It is called when a real-time update is applied.

Real-time shape logic is kept in this package, so that new sources of real-time shapes can be added
next to the existing ones.
