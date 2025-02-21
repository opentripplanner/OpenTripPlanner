package org.opentripplanner.street.model.edge;

import java.time.Duration;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.api.request.preference.CarPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

/**
 * Parking a vehicle edge.
 */
public class VehicleParkingEdge extends Edge {

  private final VehicleParking vehicleParking;

  private VehicleParkingEdge(VehicleParkingEntranceVertex vehicleParkingEntranceVertex) {
    this(vehicleParkingEntranceVertex, vehicleParkingEntranceVertex);
  }

  private VehicleParkingEdge(
    VehicleParkingEntranceVertex fromVehicleParkingEntranceVertex,
    VehicleParkingEntranceVertex toVehicleParkingEntranceVertex
  ) {
    super(fromVehicleParkingEntranceVertex, toVehicleParkingEntranceVertex);
    this.vehicleParking = fromVehicleParkingEntranceVertex.getVehicleParking();
  }

  public static VehicleParkingEdge createVehicleParkingEdge(
    VehicleParkingEntranceVertex vehicleParkingEntranceVertex
  ) {
    return connectToGraph(new VehicleParkingEdge(vehicleParkingEntranceVertex));
  }

  public static VehicleParkingEdge createVehicleParkingEdge(
    VehicleParkingEntranceVertex fromVehicleParkingEntranceVertex,
    VehicleParkingEntranceVertex toVehicleParkingEntranceVertex
  ) {
    return connectToGraph(
      new VehicleParkingEdge(fromVehicleParkingEntranceVertex, toVehicleParkingEntranceVertex)
    );
  }

  public VehicleParking getVehicleParking() {
    return vehicleParking;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof VehicleParkingEdge other) {
      return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
    }
    return false;
  }

  @Override
  public State[] traverse(State s0) {
    if (!s0.getRequest().mode().includesParking()) {
      return State.empty();
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

  protected State[] traverseUnPark(State s0) {
    if (s0.currentMode() != TraverseMode.WALK || !s0.isVehicleParked()) {
      return State.empty();
    }

    StreetMode streetMode = s0.getRequest().mode();

    if (streetMode.includesBiking()) {
      final BikePreferences bike = s0.getPreferences().bike();
      return traverseUnPark(s0, bike.parking().cost(), bike.parking().time(), TraverseMode.BICYCLE);
    } else if (streetMode.includesDriving()) {
      final CarPreferences car = s0.getPreferences().car();
      return traverseUnPark(s0, car.parking().cost(), car.parking().time(), TraverseMode.CAR);
    } else {
      return State.empty();
    }
  }

  private State[] traverseUnPark(
    State s0,
    Cost parkingCost,
    Duration parkingTime,
    TraverseMode mode
  ) {
    final StreetSearchRequest request = s0.getRequest();
    if (!vehicleParking.hasSpacesAvailable(mode, request.wheelchair())) {
      return State.empty();
    }

    StateEditor s0e = s0.edit(this);
    s0e.incrementWeight(parkingCost.toSeconds());
    s0e.incrementTimeInMilliseconds(parkingTime.toMillis());
    s0e.setVehicleParked(false, mode);

    var parkingPreferences = s0.getRequest().preferences().parking(s0.currentMode());
    addUnpreferredTagCost(parkingPreferences, s0e);

    return s0e.makeStateArray();
  }

  private State[] traversePark(State s0) {
    StreetMode streetMode = s0.getRequest().mode();
    RoutingPreferences preferences = s0.getPreferences();

    if (!streetMode.includesWalking() || s0.isVehicleParked()) {
      return State.empty();
    }

    if (streetMode.includesBiking()) {
      // Parking a rented bike is not allowed
      if (s0.isRentingVehicle()) {
        return State.empty();
      }

      return traversePark(
        s0,
        preferences.bike().parking().cost(),
        preferences.bike().parking().time()
      );
    } else if (streetMode.includesDriving()) {
      return traversePark(
        s0,
        preferences.car().parking().cost(),
        preferences.car().parking().time()
      );
    } else {
      return State.empty();
    }
  }

  private State[] traversePark(State s0, Cost parkingCost, Duration parkingTime) {
    if (!vehicleParking.hasSpacesAvailable(s0.currentMode(), s0.getRequest().wheelchair())) {
      return State.empty();
    }

    StateEditor s0e = s0.edit(this);
    s0e.incrementWeight(parkingCost.toSeconds());
    s0e.incrementTimeInMilliseconds(parkingTime.toMillis());
    s0e.setVehicleParked(true, TraverseMode.WALK);

    var parkingPreferences = s0.getRequest().preferences().parking(s0.currentMode());
    addUnpreferredTagCost(parkingPreferences, s0e);

    return s0e.makeStateArray();
  }

  private void addUnpreferredTagCost(VehicleParkingPreferences preferences, StateEditor s0e) {
    if (!preferences.preferred().matches(vehicleParking)) {
      s0e.incrementWeight(preferences.unpreferredVehicleParkingTagCost().toSeconds());
    }
  }
}
