package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.I18NString;

public class ElevatorOffboardVertex extends StreetVertex {

  private static final String LABEL_TEMPLATE = "elevator_offboard/%s/%s";
  private final String level;
  private final String label;

  public ElevatorOffboardVertex(Vertex sourceVertex, String label, String level) {
    super(sourceVertex.getX(), sourceVertex.getY());
    this.level = level;
    this.label = label;
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.string(LABEL_TEMPLATE.formatted(label, level));
  }

  @Nonnull
  @Override
  public I18NString getName() {
    return I18NString.of(label);
  }
}
