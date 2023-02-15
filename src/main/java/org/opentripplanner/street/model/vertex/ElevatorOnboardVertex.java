package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.graph.Graph;

public class ElevatorOnboardVertex extends StreetVertex {

  private final String label;

  public ElevatorOnboardVertex(Graph g, String label, double x, double y, I18NString name) {
    super(g, x, y, name);
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
