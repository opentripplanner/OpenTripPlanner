package org.opentripplanner.street.model.vertex;

/**
 * This vertex is created for a node where a barrier runs along the edge of an area.
 */
public class BarrierPassThroughVertex extends OsmVertex {

  public final long wayId;

  public BarrierPassThroughVertex(double x, double y, long nodeId, long wayId) {
    super(x, y, nodeId);
    this.wayId = wayId;
  }

  public VertexLabel getLabel() {
    return new VertexLabel.NodeOnWayLabel(nodeId, wayId);
  }
}
