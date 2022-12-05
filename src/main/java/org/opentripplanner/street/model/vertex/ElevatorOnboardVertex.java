package org.opentripplanner.street.model.vertex;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.basic.I18NString;

public class ElevatorOnboardVertex extends StreetVertex {

  public ElevatorOnboardVertex(Graph g, String label, double x, double y, I18NString name) {
    super(g, label, x, y, name);
  }
}
