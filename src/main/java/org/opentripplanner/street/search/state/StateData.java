package org.opentripplanner.street.search.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;

/**
 * StateData contains the components of search state that are unlikely to be changed as often as
 * time or weight. This avoids frequent duplication, which should have a positive impact on both
 * time and space use during searches.
 */
public class StateData implements Cloneable {

  // TODO OTP2 Many of these could be replaced by a more generic state machine implementation

  protected boolean vehicleParked;

  protected VehicleRentalState vehicleRentalState;

  protected boolean mayKeepRentedVehicleAtDestination;

  protected CarPickupState carPickupState;

  /**
   * The preferred mode, which may differ from backMode when for example walking with a bike. It may
   * also change during traversal when switching between modes as in the case of Park & Ride or Kiss
   * & Ride.
   */
  protected TraverseMode currentMode;

  /**
   * The mode that was used to traverse the backEdge
   */
  protected TraverseMode backMode;

  protected boolean backWalkingBike;

  public String vehicleRentalNetwork;

  public RentalFormFactor rentalVehicleFormFactor;

  /** This boolean is set to true upon transition from a normal street to a no-through-traffic street. */
  protected boolean enteredNoThroughTrafficArea;

  protected boolean insideNoRentalDropOffArea = false;
  public Set<String> noRentalDropOffZonesAtStartOfReverseSearch = Set.of();

  /** Private constructor, use static methods to get a set of initial states. */
  protected StateData(StreetMode requestMode) {
    currentMode =
      switch (requestMode) {
        // when renting or using a flex vehicle, you start on foot until you have found the vehicle
        case NOT_SET, WALK, BIKE_RENTAL, SCOOTER_RENTAL, CAR_RENTAL, FLEXIBLE -> TraverseMode.WALK;
        // when cycling all the way or to a stop, you start on your own bike
        case BIKE, BIKE_TO_PARK -> TraverseMode.BICYCLE;
        // when driving (not car rental) you start in your own car or your driver's car
        case CAR, CAR_TO_PARK, CAR_PICKUP, CAR_HAILING -> TraverseMode.CAR;
      };
  }

  /**
   * Returns a set of initial StateDatas based on the options from the RouteRequest
   */
  public static List<StateData> getInitialStateDatas(StreetSearchRequest request) {
    return getInitialStateDatas(request, StateData::new);
  }

  /**
   * Returns a set of initial StateDatas based on the options from the RouteRequest, with a custom
   * StateData implementation.
   */
  public static List<StateData> getInitialStateDatas(
    StreetSearchRequest request,
    Function<StreetMode, StateData> stateDataConstructor
  ) {
    return getInitialStateDatas(
      request.mode(),
      request.arriveBy(),
      request.rental().allowArrivingInRentedVehicleAtDestination(),
      false,
      stateDataConstructor
    );
  }

  /**
   * Returns an initial StateData based on the options from the RouteRequest. This returns always
   * only a single state, which is considered the "base case"
   */
  public static StateData getInitialStateData(StreetSearchRequest request) {
    var stateDatas = getInitialStateDatas(
      request.mode(),
      request.arriveBy(),
      request.rental().allowArrivingInRentedVehicleAtDestination(),
      true,
      StateData::new
    );
    if (stateDatas.size() != 1) {
      throw new IllegalStateException("Unable to create only a single state");
    }
    return stateDatas.get(0);
  }

  /**
   * @param forceSingleState Is a hack to force only an single state to be returned. This is useful
   *                         mostly in tests, which test a single State
   */
  private static List<StateData> getInitialStateDatas(
    StreetMode requestMode,
    boolean arriveBy,
    boolean allowArrivingInRentedVehicleAtDestination,
    boolean forceSingleState,
    Function<StreetMode, StateData> stateDataConstructor
  ) {
    List<StateData> res = new ArrayList<>();
    var proto = stateDataConstructor.apply(requestMode);

    // carPickup searches may start and end in two distinct states:
    //   - CAR / IN_CAR where pickup happens directly at the bus stop
    //   - WALK / WALK_FROM_DROP_OFF or WALK_TO_PICKUP for cases with an initial walk
    // For forward/reverse searches to be symmetric both initial states need to be created.
    if (requestMode.includesPickup()) {
      if (!forceSingleState) {
        var inCarPickupStateData = proto.clone();
        inCarPickupStateData.carPickupState = CarPickupState.IN_CAR;
        inCarPickupStateData.currentMode = TraverseMode.CAR;
        res.add(inCarPickupStateData);
      }
      var walkingPickupStateData = proto.clone();
      walkingPickupStateData.carPickupState =
        arriveBy ? CarPickupState.WALK_FROM_DROP_OFF : CarPickupState.WALK_TO_PICKUP;
      walkingPickupStateData.currentMode = TraverseMode.WALK;
      res.add(walkingPickupStateData);
    }
    // Vehicle rental searches may end in four states (see State#isFinal()):
    // When searching forward:
    //   - RENTING_FROM_STATION when allowKeepingRentedVehicleAtDestination is set
    //   - RENTING_FLOATING
    //   - HAVE_RENTED
    // When searching backwards:
    //   - BEFORE_RENTING
    else if (requestMode.includesRenting()) {
      if (arriveBy) {
        if (!forceSingleState) {
          if (allowArrivingInRentedVehicleAtDestination) {
            var keptVehicleStateData = proto.clone();
            keptVehicleStateData.vehicleRentalState = VehicleRentalState.RENTING_FROM_STATION;
            keptVehicleStateData.currentMode = TraverseMode.BICYCLE;
            keptVehicleStateData.mayKeepRentedVehicleAtDestination = true;
            res.add(keptVehicleStateData);
          }
          var floatingRentalStateData = proto.clone();
          floatingRentalStateData.vehicleRentalState = VehicleRentalState.RENTING_FLOATING;
          floatingRentalStateData.rentalVehicleFormFactor = toFormFactor(requestMode);
          floatingRentalStateData.currentMode = TraverseMode.BICYCLE;
          res.add(floatingRentalStateData);
        }
        var stationReturnedStateData = proto.clone();
        stationReturnedStateData.vehicleRentalState = VehicleRentalState.HAVE_RENTED;
        stationReturnedStateData.currentMode = TraverseMode.WALK;
        res.add(stationReturnedStateData);
      } else {
        var beforeRentalStateData = proto.clone();
        beforeRentalStateData.vehicleRentalState = VehicleRentalState.BEFORE_RENTING;
        res.add(beforeRentalStateData);
      }
    }
    // If the itinerary is to begin with a car that is parked for transit the initial state is
    //   - In arriveBy searches is with the car already "parked" and in WALK mode
    //   - In departAt searches, we are in CAR mode and "unparked".
    else if (requestMode.includesParking()) {
      var parkAndRideStateData = proto.clone();
      parkAndRideStateData.vehicleParked = arriveBy;
      parkAndRideStateData.currentMode =
        parkAndRideStateData.vehicleParked
          ? TraverseMode.WALK
          : requestMode.includesBiking() ? TraverseMode.BICYCLE : TraverseMode.CAR;
      res.add(parkAndRideStateData);
    } else {
      res.add(proto.clone());
    }

    return res;
  }

  private static RentalFormFactor toFormFactor(StreetMode streetMode) {
    return switch (streetMode) {
      case BIKE_RENTAL -> RentalFormFactor.BICYCLE;
      case SCOOTER_RENTAL -> RentalFormFactor.SCOOTER;
      case CAR_RENTAL -> RentalFormFactor.CAR;
      // there is no default here so you get a compiler error when you add a new value to the enum
      case NOT_SET,
        WALK,
        BIKE,
        BIKE_TO_PARK,
        CAR,
        CAR_TO_PARK,
        CAR_PICKUP,
        CAR_HAILING,
        FLEXIBLE -> throw new IllegalStateException(
        "Cannot convert street mode %s to a form factor".formatted(streetMode)
      );
    };
  }

  public StateData clone() {
    try {
      return (StateData) super.clone();
    } catch (CloneNotSupportedException e1) {
      throw new IllegalStateException("This is not happening");
    }
  }
}
