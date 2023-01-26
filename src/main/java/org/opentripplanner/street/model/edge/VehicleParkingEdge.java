package org.opentripplanner.street.model.edge;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.api.request.preference.CarPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

/**
 * Parking a vehicle edge.
 */
public class VehicleParkingEdge extends Edge {

  private final VehicleParking vehicleParking;

  public VehicleParkingEdge(VehicleParkingEntranceVertex vehicleParkingEntranceVertex) {
    this(vehicleParkingEntranceVertex, vehicleParkingEntranceVertex);
  }

  public VehicleParkingEdge(
    VehicleParkingEntranceVertex fromVehicleParkingEntranceVertex,
    VehicleParkingEntranceVertex toVehicleParkingEntranceVertex
  ) {
    super(fromVehicleParkingEntranceVertex, toVehicleParkingEntranceVertex);
    this.vehicleParking = fromVehicleParkingEntranceVertex.getVehicleParking();
  }

  public VehicleParking getVehicleParking() {
    return vehicleParking;
  }

  public boolean equals(Object o) {
    if (o instanceof VehicleParkingEdge) {
      VehicleParkingEdge other = (VehicleParkingEdge) o;
      return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
    }
    return false;
  }

  public String toString() {
    return "VehicleParkingEdge(" + fromv + " -> " + tov + ")";
  }

  @Override
  public State traverse(State s0) {
    if (!s0.getRequest().mode().includesParking()) {
      return null;
    }

    if (s0.getRequest().arriveBy()) {
      return traverseUnPark(s0);
    } else {
      return traversePark(s0);
    }
  }

  @Override
  public I18NString getName() {
    return getToVertex().getName();
  }

  @Override
  public boolean hasBogusName() {
    return false;
  }

  @Override
  public LineString getGeometry() {
    return null;
  }

  @Override
  public double getDistanceMeters() {
    return 0;
  }

  protected State traverseUnPark(State s0) {
    if (s0.getNonTransitMode() != TraverseMode.WALK || !s0.isVehicleParked()) {
      return null;
    }

    StreetMode streetMode = s0.getRequest().mode();

    if (streetMode.includesBiking()) {
      final BikePreferences bike = s0.getPreferences().bike();
      return traverseUnPark(s0, bike.parkCost(), bike.parkTime(), TraverseMode.BICYCLE);
    } else if (streetMode.includesDriving()) {
      final CarPreferences car = s0.getPreferences().car();
      return traverseUnPark(s0, car.parkCost(), car.parkTime(), TraverseMode.CAR);
    } else {
      return null;
    }
  }

  private State traverseUnPark(State s0, int parkingCost, int parkingTime, TraverseMode mode) {
    if (
      !vehicleParking.hasSpacesAvailable(
        mode,
        s0.getRequest().wheelchair(),
        s0.getPreferences().parking().useAvailabilityInformation()
      )
    ) {
      return null;
    }

    StateEditor s0e = s0.edit(this);
    s0e.incrementWeight(parkingCost);
    s0e.incrementTimeInSeconds(parkingTime);
    s0e.setVehicleParked(false, mode);
    return s0e.makeState();
  }

  private State traversePark(State s0) {
    StreetMode streetMode = s0.getRequest().mode();
    RoutingPreferences preferences = s0.getPreferences();

    if (!streetMode.includesWalking() || s0.isVehicleParked()) {
      return null;
    }

    if (streetMode.includesBiking()) {
      // Parking a rented bike is not allowed
      if (s0.isRentingVehicle()) {
        return null;
      }

      return traversePark(s0, preferences.bike().parkCost(), preferences.bike().parkTime());
    } else if (streetMode.includesDriving()) {
      return traversePark(s0, preferences.car().parkCost(), preferences.car().parkTime());
    } else {
      return null;
    }
  }

  private State traversePark(State s0, int parkingCost, int parkingTime) {
    if (
      !vehicleParking.hasSpacesAvailable(
        s0.getNonTransitMode(),
        s0.getRequest().wheelchair(),
        s0.getPreferences().parking().useAvailabilityInformation()
      )
    ) {
      return null;
    }

    StateEditor s0e = s0.edit(this);
    s0e.incrementWeight(parkingCost);
    s0e.incrementTimeInSeconds(parkingTime);
    s0e.setVehicleParked(true, TraverseMode.WALK);
    return s0e.makeState();
  }
}
