package org.opentripplanner.street.model.vertex;

/**
 * A vertex coming from OpenStreetMap.
 * <p>
 * This class marks something that comes from the street network itself.
 */
public class OsmVertex extends IntersectionVertex {

  /** The OSM node ID from whence this came */
  public final long nodeId;

  public OsmVertex(double x, double y, long nodeId) {
    super(x, y);
    this.nodeId = nodeId;
  }

  public OsmVertex(
    double x,
    double y,
    long nodeId,
    boolean hasHighwayTrafficLight,
    boolean hasCrossingTrafficLight
  ) {
    super(x, y, null, hasHighwayTrafficLight, hasCrossingTrafficLight);
    this.nodeId = nodeId;
  }

  @Override
  public VertexLabel getLabel() {
    return new VertexLabel.OsmNodeLabel(nodeId);
  }
}
