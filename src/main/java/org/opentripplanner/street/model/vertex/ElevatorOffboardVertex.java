package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.NonLocalizedString;

public class ElevatorOffboardVertex extends StreetVertex {

  private final String level;
  private final VertexLabel label;

  public ElevatorOffboardVertex(Vertex sourceVertex, VertexLabel label, String level) {
    super(sourceVertex.getX(), sourceVertex.getY(), NonLocalizedString.ofNullable(level));
    this.level = level;
    this.label = label;
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.string("elevator_offboard/%s/%s".formatted(label.toString(), level));
  }
}
