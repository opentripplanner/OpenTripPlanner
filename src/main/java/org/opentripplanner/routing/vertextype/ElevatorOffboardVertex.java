package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.basic.I18NString;

public class ElevatorOffboardVertex extends StreetVertex {

  private static final long serialVersionUID = 20120209L;

  public ElevatorOffboardVertex(Graph g, String label, double x, double y, I18NString name) {
    super(g, label, x, y, name);
  }
}
