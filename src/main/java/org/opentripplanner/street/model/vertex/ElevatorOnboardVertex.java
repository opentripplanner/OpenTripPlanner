package org.opentripplanner.street.model.vertex;

import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.NonLocalizedString;

public class ElevatorOnboardVertex extends StreetVertex {

  private final Vertex sourceVertex;
  private final String level;

  public ElevatorOnboardVertex(Vertex sourceVertex, @Nullable  String level) {
    super(sourceVertex.getX(), sourceVertex.getY(), NonLocalizedString.ofNullable(level));
    this.sourceVertex = sourceVertex;
    this.level = level;
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.string("elevator_onboard/%s/%s".formatted(sourceVertex.getLabel(), level));
  }
}
