package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.graph.Graph;

/**
 * A vertex coming from OpenStreetMap.
 * <p>
 * This class marks something that comes from the street network itself. It is used for linking
 * origins in Analyst to ensure that they are linked to the same locations regardless of changes in
 * the transit network between (or eventually within) graphs.
 */
public class OsmVertex extends IntersectionVertex {

  private static final String LABEL_FORMAT = "osm:node:%d";
  /** The OSM node ID from whence this came */
  public final long nodeId;

  public OsmVertex(Graph g, double x, double y, long nodeId) {
    this(g, x, y, nodeId, false, false);
  }

  public OsmVertex(
    Graph g,
    double x,
    double y,
    long nodeId,
    boolean hasHighwayTrafficLight,
    boolean hasCrossingTrafficLight
  ) {
    super(
      g,
      x,
      y,
      new NonLocalizedString(LABEL_FORMAT.formatted(nodeId)),
      hasHighwayTrafficLight,
      hasCrossingTrafficLight
    );
    this.nodeId = nodeId;
  }

  @Override
  public String getLabel() {
    return LABEL_FORMAT.formatted(nodeId);
  }
}
