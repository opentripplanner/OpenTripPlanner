package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.graph.Graph;

public class ElevatorOnboardVertex extends StreetVertex {

  public ElevatorOnboardVertex(Graph g, String label, double x, double y, I18NString name) {
    super(g, label, x, y, name);
  }
}
