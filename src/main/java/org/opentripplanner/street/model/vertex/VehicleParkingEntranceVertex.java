package org.opentripplanner.street.model.vertex;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.I18NString;
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

  public VehicleParkingEntranceVertex(VehicleParkingEntrance parkingEntrance) {
    super(parkingEntrance.getCoordinate().longitude(), parkingEntrance.getCoordinate().latitude());
    this.parkingEntrance = parkingEntrance;
  }

  @Nonnull
  @Override
  public I18NString getName() {
    return Objects.requireNonNullElse(parkingEntrance.getName(), NO_NAME);
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.string("Vehicle parking " + parkingEntrance.getEntranceId());
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
