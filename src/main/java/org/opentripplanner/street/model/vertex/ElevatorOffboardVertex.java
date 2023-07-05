package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.NonLocalizedString;

public class ElevatorOffboardVertex extends StreetVertex {

  private final Vertex sourceVertex;
  private final String level;

  public ElevatorOffboardVertex(Vertex sourceVertex, String level) {
    super(sourceVertex.getX(), sourceVertex.getY(), NonLocalizedString.ofNullable(level));
    this.sourceVertex = sourceVertex;
    this.level=level;
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.string("elevator_offboard/%s/%s".formatted(sourceVertex.getLabel(), level));
  }
}
