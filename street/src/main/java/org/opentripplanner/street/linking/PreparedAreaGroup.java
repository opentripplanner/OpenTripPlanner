package org.opentripplanner.street.linking;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.operation.relateng.RelateNG;
import org.locationtech.jts.operation.relateng.RelatePredicate;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.AreaGroup;

/**
 * An {@link AreaGroup} together with a lazily built, reusable spatial index over its polygon, used to
 * answer the repeated {@code contains} checks performed while linking a vertex to the area's
 * visibility vertices.
 * <p>
 * A plain {@code Polygon.contains(...)} rebuilds a sweep-line index over the polygon boundary on
 * every call. By preparing the polygon once with {@link RelateNG} and reusing the cached index
 * across all candidate checks, we build that index a single time per linking operation. RelateNG is
 * also tolerant of invalid / self-intersecting OSM polygons (it does not throw
 * {@code TopologyException}).
 * <p>
 * The index is built lazily on the first {@link #containsSegment(Coordinate, Coordinate)} call, so
 * the forced-edge linking paths — which skip the boundary check — never pay for it. An instance is
 * created and used within a single linking call and never escapes that thread, so the unsynchronized
 * lazy field is not a concern.
 */
final class PreparedAreaGroup {

  private static final GeometryFactory GEOMETRY_FACTORY = GeometryUtils.getGeometryFactory();

  /**
   * Factor by which a candidate segment is shrunk at each end before the containment test. Floating
   * point math cannot represent boundaries precisely, so the segment between two boundary points is
   * pulled slightly inward to make the test robust.
   */
  private static final double AREA_INTERSECTION_SHRINKING = 0.0001;

  private final AreaGroup areaGroup;
  private RelateNG prepared;

  PreparedAreaGroup(AreaGroup areaGroup) {
    this.areaGroup = areaGroup;
  }

  AreaGroup areaGroup() {
    return areaGroup;
  }

  /**
   * Whether the area polygon contains the segment between the two coordinates. The segment is shrunk
   * slightly at both ends first, so that a segment connecting two boundary points is not rejected by
   * floating point imprecision at the boundary. Reuses a cached prepared index across calls.
   */
  boolean containsSegment(Coordinate from, Coordinate to) {
    if (prepared == null) {
      prepared = RelateNG.prepare(areaGroup.getGeometry());
    }
    return prepared.evaluate(createShrunkLine(from, to), RelatePredicate.contains());
  }

  private static LineString createShrunkLine(Coordinate from, Coordinate to) {
    var dx = AREA_INTERSECTION_SHRINKING * (to.x - from.x);
    var dy = AREA_INTERSECTION_SHRINKING * (to.y - from.y);
    var c1 = new Coordinate(from.x + dx, from.y + dy);
    var c2 = new Coordinate(to.x - dx, to.y - dy);
    return GEOMETRY_FACTORY.createLineString(new Coordinate[] { c1, c2 });
  }
}
