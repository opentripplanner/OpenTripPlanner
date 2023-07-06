package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.NonLocalizedString;

public class ElevatorOffboardVertex extends StreetVertex {

  private static final String LABEL_TEMPLATE = "elevator_offboard/%s/%s";
  private final String level;
  private final String label;

  public ElevatorOffboardVertex(Vertex sourceVertex, String label, String level) {
    super(sourceVertex.getX(), sourceVertex.getY(), NonLocalizedString.ofNullable(level));
    this.level = level;
    this.label = label;
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.string(LABEL_TEMPLATE.formatted(label, level));
  }
}
