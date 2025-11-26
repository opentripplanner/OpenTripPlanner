package org.opentripplanner.ext.edgenaming;

import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.HashGridSpatialIndex;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.model.edge.StreetEdge;

/**
 * Helper class for collecting {@link OsmWay}/{@link StreetEdge} pairs in a {@link HashGridSpatialIndex}.
 */
class StreetEdgeIndex {

  private final HashGridSpatialIndex<EdgeOnLevel> index = new HashGridSpatialIndex<>();

  /**
   * Adds an entry to a geospatial index.
   */
  public void add(OsmWay way, StreetEdgePair pair, Set<OsmLevel> levels) {
    add(way, pair, levels, Integer.MAX_VALUE);
  }

  /**
   * Adds an entry to a geospatial index if its length is less than a threshold.
   */
  public void add(OsmWay way, StreetEdgePair pair, Set<OsmLevel> levels, int maxLengthMeters) {
    // We generate two edges for each osm way: one there and one back. This spatial index only
    // needs to contain one item for each road segment with a unique geometry and name, so we
    // add only one of the two edges.
    var edge = pair.pickAny();
    if (edge.getDistanceMeters() <= maxLengthMeters) {
      index.insert(edge.getGeometry().getEnvelopeInternal(), new EdgeOnLevel(way, edge, levels));
    }
  }

  public List<EdgeOnLevel> query(Geometry buffer) {
    return index.query(buffer.getEnvelopeInternal());
  }
}
