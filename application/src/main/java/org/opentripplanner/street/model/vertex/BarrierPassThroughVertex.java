package org.opentripplanner.street.model.vertex;

/**
 * This vertex is created for a node where a barrier runs along the edge of an area.
 */
public class BarrierPassThroughVertex extends OsmVertex {

  private final OsmEntityType osmEntityType;
  private final long entityId;

  public BarrierPassThroughVertex(
    double x,
    double y,
    long nodeId,
    OsmEntityType osmEntityType,
    long entityId
  ) {
    super(x, y, nodeId);
    this.osmEntityType = osmEntityType;
    this.entityId = entityId;
  }

  public long getEntityId() {
    return entityId;
  }

  public VertexLabel getLabel() {
    return new VertexLabel.VertexWithEntityLabel(nodeId, osmEntityType, entityId);
  }
}
