package org.opentripplanner.street.model.vertex;

import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.NonLocalizedString;

public class ElevatorOnboardVertex extends StreetVertex {

  private final Vertex sourceVertex;
  private final String level;
  private final VertexLabel label;

  public ElevatorOnboardVertex(Vertex sourceVertex, VertexLabel label, @Nullable String level) {
    super(sourceVertex.getX(), sourceVertex.getY(), NonLocalizedString.ofNullable(level));
    this.sourceVertex = sourceVertex;
    this.level = level;
    this.label = label;
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.string("elevator_onboard/%s/%s".formatted(label, level));
  }
}
