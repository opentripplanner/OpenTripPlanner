package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.I18NString;

public class ElevatorHopVertex extends StreetVertex {

  private static final String LABEL_TEMPLATE = "elevator/%s";
  private final String label;

  public ElevatorHopVertex(Vertex sourceVertex, String label) {
    super(sourceVertex.getX(), sourceVertex.getY());
    this.label = label;
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.string(LABEL_TEMPLATE.formatted(label));
  }

  @Override
  public I18NString getName() {
    return I18NString.of(label);
  }
}
