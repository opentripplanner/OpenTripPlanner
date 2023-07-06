package org.opentripplanner.street.model.vertex;

import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.NonLocalizedString;

public class ElevatorOnboardVertex extends StreetVertex {

  private static final String LABEL_TEMPLATE = "elevator_onboard/%s/%s";
  private final String level;
  private final String label;

  public ElevatorOnboardVertex(Vertex sourceVertex, String label, @Nullable String level) {
    super(sourceVertex.getX(), sourceVertex.getY(), NonLocalizedString.ofNullable(level));
    this.level = level;
    this.label = label;
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.string(LABEL_TEMPLATE.formatted(label, level));
  }
}
