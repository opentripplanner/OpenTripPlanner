package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.geometry.WgsCoordinate;

/**
 * A vertex that represents an OSM elevator node in conjunction with a connected OSM entity. This
 * class can almost be described as representing one level of an elevator vertex. However, due to
 * a lack of level information in OSM for entities connected to an elevator node, going by the data
 * means that two of these vertices could be on the same level.
 * <p>
 * Two OSM elevator vertices on the same level need to be separate! The reason for this is that
 * traversing an elevator node only makes sense if you use the elevator. Having a shared elevator
 * vertex with entities on the same level creates a teleportation device from one level to another
 * with bad data.
 * <p>
 * This is a separate class in order to conserve memory as only a tiny percentage of vertices
 * actually has level information.
 */
public class OsmElevatorVertex extends OsmVertex {

  private final OsmEntityType osmEntityType;
  private final long entityId;

  public OsmElevatorVertex(
    long nodeId,
    WgsCoordinate coordinate,
    OsmEntityType osmEntityType,
    long entityId
  ) {
    super(coordinate.longitude(), coordinate.latitude(), nodeId);
    this.osmEntityType = osmEntityType;
    this.entityId = entityId;
  }

  @Override
  public VertexLabel getLabel() {
    return new VertexLabel.VertexWithEntityLabel(nodeId(), osmEntityType, entityId);
  }
}
