package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.astar.model.Vertex;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.street.model.edge.StreetVehicleRentalLink;
import org.opentripplanner.street.model.edge.VehicleRentalEdge;

/**
 * A vertex for a rental vehicle or station. It is connected to the streets by a
 * {@link StreetVehicleRentalLink}. To allow transitions on and
 * off a vehicle, it has {@link VehicleRentalEdge} loop edges.
 */
public class VehicleRentalPlaceVertex extends Vertex {

  private static final long serialVersionUID = 2L;

  private VehicleRentalPlace station;

  public VehicleRentalPlaceVertex(Graph g, VehicleRentalPlace station) {
    super(
      g,
      "vehicle rental station " + station.getId(),
      station.getLongitude(),
      station.getLatitude(),
      station.getName()
    );
    this.station = station;
  }

  public VehicleRentalPlace getStation() {
    return station;
  }

  public void setStation(VehicleRentalPlace station) {
    this.station = station;
  }
}
