package org.opentripplanner.street.model.vertex;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingEntrance;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;

/**
 * A vertex for a vehicle parking entrance.
 * <p>
 * Connected to streets by {@link StreetVehicleParkingLink}.
 * Transition for parking the bike is handled by {@link VehicleParkingEdge}.
 */
public class VehicleParkingEntranceVertex extends Vertex {

  private final VehicleParkingEntrance parkingEntrance;

  public VehicleParkingEntranceVertex(Graph g, VehicleParkingEntrance parkingEntrance) {
    super(
      g,
      "Vehicle parking " + parkingEntrance.getEntranceId(),
      parkingEntrance.getCoordinate().longitude(),
      parkingEntrance.getCoordinate().latitude(),
      parkingEntrance.getName()
    );
    this.parkingEntrance = parkingEntrance;
  }

  public VehicleParkingEntrance getParkingEntrance() {
    return parkingEntrance;
  }

  public VehicleParking getVehicleParking() {
    return parkingEntrance.getVehicleParking();
  }

  public boolean isCarAccessible() {
    return parkingEntrance.isCarAccessible();
  }

  public boolean isWalkAccessible() {
    return parkingEntrance.isWalkAccessible();
  }
}
