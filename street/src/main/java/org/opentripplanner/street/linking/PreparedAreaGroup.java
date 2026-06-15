package org.opentripplanner.street.linking;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.relateng.RelateNG;
import org.locationtech.jts.operation.relateng.RelatePredicate;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.Area;
import org.opentripplanner.street.model.edge.AreaGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AreaGroup} together with lazily built, reusable spatial indexes over its polygons, used
 * to answer the repeated geometric queries performed while linking a vertex to the area's visibility
 * vertices. An instance is created once per linking call and reused across all candidate visibility
 * edges, so each index is built a single time and amortized over those edges. It never escapes the
 * linking thread, so the unsynchronized lazy fields are not a concern.
 */
final class PreparedAreaGroup {

  private static final Logger LOG = LoggerFactory.getLogger(PreparedAreaGroup.class);

  private static final GeometryFactory GEOMETRY_FACTORY = GeometryUtils.getGeometryFactory();

  /**
   * Factor by which a candidate segment is shrunk at each end before the containment test. Floating
   * point math cannot represent boundaries precisely, so the segment between two boundary points is
   * pulled slightly inward to make the test robust.
   */
  private static final double AREA_INTERSECTION_SHRINKING = 0.0001;

  private final AreaGroup areaGroup;
  private RelateNG prepared;
  private PreparedGeometry[] preparedAreas;

  PreparedAreaGroup(AreaGroup areaGroup) {
    this.areaGroup = areaGroup;
  }

  AreaGroup areaGroup() {
    return areaGroup;
  }

  /**
   * Whether the area group polygon contains the segment between the two coordinates. The segment is
   * shrunk slightly at both ends first, so that a segment connecting two boundary points is not
   * rejected by floating point imprecision at the boundary. Reuses a cached prepared index across
   * calls. A plain {@code Polygon.contains(...)} would rebuild a sweep-line index over the polygon
   * boundary on every call; {@link RelateNG} builds it once and is also tolerant of invalid /
   * self-intersecting OSM polygons (it does not throw {@code TopologyException}).
   */
  boolean containsSegment(Coordinate from, Coordinate to) {
    if (prepared == null) {
      prepared = RelateNG.prepare(areaGroup.getGeometry());
    }
    return prepared.evaluate(createShrunkLine(from, to), RelatePredicate.contains());
  }

  /**
   * The sub-areas of this group that the visibility {@code line} crosses. A multi-area group's
   * sub-areas tile/overlap to cover the group, so a visibility edge routinely crosses more than one;
   * the caller merges their properties (see {@link AreaEdgeProperties#merge(List)}). The crossing test
   * uses a per-sub-area {@link PreparedGeometry}, built once and reused across the group's edges; it
   * allocates no overlay geometry. The line is shrunk slightly at both ends first (as
   * {@link #containsSegment} does): its endpoints are visibility vertices sitting on the area boundary,
   * so an un-shrunk {@code intersects} would also match any neighbouring sub-area the edge merely
   * touches at that shared endpoint without ever crossing its interior — grafting that neighbour's
   * (worse) properties onto the edge. Shrinking removes those 0-D boundary touches while keeping every
   * genuine 1-D crossing. The returned list is never empty: if no sub-area is actually crossed (a point
   * force-linked from outside the group) the first sub-area is returned as a fallback so the edge still
   * receives some properties.
   *
   * @param line the visibility edge segment
   */
  List<Area> areasCrossedBy(LineString line) {
    List<Area> areas = areaGroup.getAreas();
    if (areas.size() == 1) {
      return areas;
    }
    if (preparedAreas == null) {
      preparedAreas = new PreparedGeometry[areas.size()];
      for (int i = 0; i < areas.size(); i++) {
        preparedAreas[i] = PreparedGeometryFactory.prepare(areas.get(i).getGeometry());
      }
    }
    LineString probe = createShrunkLine(
      line.getCoordinateN(0),
      line.getCoordinateN(line.getNumPoints() - 1)
    );
    List<Area> crossed = new ArrayList<>(areas.size());
    for (int i = 0; i < areas.size(); i++) {
      if (preparedAreas[i].intersects(probe)) {
        crossed.add(areas.get(i));
      }
    }
    if (crossed.isEmpty()) {
      LOG.warn("No intersecting area found. This may indicate a bug.");
      return List.of(areas.getFirst());
    }
    return crossed;
  }

  private static LineString createShrunkLine(Coordinate from, Coordinate to) {
    var dx = AREA_INTERSECTION_SHRINKING * (to.x - from.x);
    var dy = AREA_INTERSECTION_SHRINKING * (to.y - from.y);
    var c1 = new Coordinate(from.x + dx, from.y + dy);
    var c2 = new Coordinate(to.x - dx, to.y - dy);
    return GEOMETRY_FACTORY.createLineString(new Coordinate[] { c1, c2 });
  }
}
