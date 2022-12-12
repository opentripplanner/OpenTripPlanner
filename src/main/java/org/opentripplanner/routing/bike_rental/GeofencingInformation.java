package org.opentripplanner.routing.bike_rental;

import java.util.Set;
import org.opentripplanner.street.model.edge.Edge;

public class GeofencingInformation {

  private final String networkId;
  private final Set<Edge> permittedDropOffEdges;

  public String getNetworkId() {
    return networkId;
  }

  public GeofencingInformation(String networkId, Set<Edge> permittedDropOffEdges) {
    this.networkId = networkId;
    this.permittedDropOffEdges = permittedDropOffEdges;
  }

  public boolean canDropOffVehicle(Edge e) {
    return permittedDropOffEdges.contains(e);
  }
}
