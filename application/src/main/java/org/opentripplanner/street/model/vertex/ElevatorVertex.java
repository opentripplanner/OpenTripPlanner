package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.I18NString;

public class ElevatorVertex extends StreetVertex {

  private static final String LABEL_TEMPLATE = "elevator/%s/%s";
  private final double level;
  private final String label;

  public ElevatorVertex(Vertex sourceVertex, String label, double level) {
    super(sourceVertex.getX(), sourceVertex.getY());
    this.level = level;
    this.label = label;
  }

  /**
   * Numerical level value from e.g. OSM or GTFS.
   */
  public double getLevel() {
    return level;
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.string(LABEL_TEMPLATE.formatted(label, level));
  }

  @Override
  public I18NString getName() {
    return I18NString.of(label);
  }
}
