package org.opentripplanner.street.model.vertex;

/**
 * This vertex is created for a node on a particular way
 */
public class OsmVertexOnWay extends OsmVertex {

  public final long wayId;

  public OsmVertexOnWay(double x, double y, long nodeId, long wayId) {
    super(x, y, nodeId);
    this.wayId = wayId;
  }

  public VertexLabel getLabel() {
    return new VertexLabel.NodeOnWayLabel(nodeId, wayId);
  }
}
