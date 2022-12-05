package org.opentripplanner.street.model.vertex;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.basic.NonLocalizedString;

public class SimpleVertex extends StreetVertex {

  public SimpleVertex(Graph g, String label, double lat, double lon) {
    super(g, label, lon, lat, new NonLocalizedString(label));
  }
}
