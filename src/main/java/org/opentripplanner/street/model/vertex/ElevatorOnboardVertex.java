package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;

public class ElevatorOnboardVertex extends StreetVertex {

  private static final String LABEL_TEMPLATE = "elevator_onboard/%s/%s";
  private final String level;
  private final String label;

  public ElevatorOnboardVertex(Vertex sourceVertex, String label, @Nullable String level) {
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
