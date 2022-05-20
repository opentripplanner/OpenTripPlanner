package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;

/**
 * A vertex for a rental vehicle or station. It is connected to the streets by a
 * {@link org.opentripplanner.routing.edgetype.StreetVehicleRentalLink}. To allow transitions on and
 * off a vehicle, it has {@link org.opentripplanner.routing.edgetype.VehicleRentalEdge} loop edges.
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
