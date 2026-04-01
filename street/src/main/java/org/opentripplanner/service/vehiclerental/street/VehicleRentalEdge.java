package org.opentripplanner.service.vehiclerental.street;

import java.time.Instant;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType.PropulsionType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.street.mapping.StreetModeToRentalTraverseModeMapper;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

/**
 * Renting or dropping off a rented vehicle edge.
 *
 * @author laurent
 */
public class VehicleRentalEdge extends Edge {

  public final RentalFormFactor formFactor;

  private VehicleRentalEdge(VehicleRentalPlaceVertex vertex, RentalFormFactor formFactor) {
    super(vertex, vertex);
    this.formFactor = formFactor;
  }

  public static VehicleRentalEdge createVehicleRentalEdge(
    VehicleRentalPlaceVertex vertex,
    RentalFormFactor formFactor
  ) {
    return connectToGraph(new VehicleRentalEdge(vertex, formFactor));
  }

  @Override
  public State[] traverse(State s0) {
    if (!s0.getRequest().mode().includesRenting()) {
      return State.empty();
    }

    if (!isFormFactorAllowed(s0.getRequest().mode(), formFactor)) {
      return State.empty();
    }

    StateEditor s1 = s0.edit(this);

    VehicleRentalPlaceVertex stationVertex = (VehicleRentalPlaceVertex) tov;
    VehicleRentalPlace station = stationVertex.getStation();
    String network = station.network();
    var request = s0.getRequest().rental(formFactor.traverseMode);
    boolean realtimeAvailability = request.useAvailabilityInformation();

    if (station.networkIsNotAllowed(request)) {
      return State.empty();
    }

    boolean pickedUp;
    if (s0.getRequest().arriveBy()) {
      switch (s0.getVehicleRentalState()) {
        case BEFORE_RENTING -> {
          return State.empty();
        }
        case HAVE_RENTED -> {
          if (!station.canDropOffFormFactor(formFactor, realtimeAvailability)) {
            return State.empty();
          }
          s1.dropOffRentedVehicleAtStation(formFactor, getPropulsionType(station), network, true);
          pickedUp = false;
        }
        case RENTING_FLOATING -> {
          // a very special case: an arriveBy search has started in a no-drop-off zone.
          // in such a case we mark this case in the state that speculatively picks up a vehicle
          // when leaving the no-drop-off zone (has no network) and check it here so that we cannot
          // begin the rental.
          // reminder: in an arriveBy search we traverse backwards so beginFloatingVehicle means
          // traversing from renting to walking.
          if (
            s0.stateData.noRentalDropOffZonesAtStartOfReverseSearch.contains(network) ||
            !station.availablePickupFormFactors(realtimeAvailability).contains(formFactor)
          ) {
            return State.empty();
          }
          if (station.isFloatingVehicle()) {
            if (!isVehicleAvailableDuringRentalPeriod(s0, station)) {
              return State.empty();
            }
            s1.beginFloatingVehicleRenting(formFactor, getPropulsionType(station), network, true);
            pickedUp = true;
          } else {
            return State.empty();
          }
        }
        case RENTING_FROM_STATION -> {
          if (
            (realtimeAvailability && !station.allowPickupNow()) ||
            !station.availablePickupFormFactors(realtimeAvailability).contains(formFactor)
          ) {
            return State.empty();
          }
          // For arriveBy searches mayKeepRentedVehicleAtDestination is only set in State#getInitialStates(),
          // and so here it is checked if this bicycle could have been kept at the destination
          if (
            s0.mayKeepRentedVehicleAtDestination() &&
            !station.isArrivingInRentalVehicleAtDestinationAllowed()
          ) {
            return State.empty();
          }
          if (!hasCompatibleNetworks(network, s0.getVehicleRentalNetwork())) {
            return State.empty();
          }
          s1.beginVehicleRentingAtStation(
            formFactor,
            getPropulsionType(station),
            network,
            false,
            true
          );
          pickedUp = true;
        }
        default -> throw new IllegalStateException();
      }
    } else {
      switch (s0.getVehicleRentalState()) {
        case BEFORE_RENTING -> {
          if (
            (realtimeAvailability && !station.allowPickupNow()) ||
            !station.availablePickupFormFactors(realtimeAvailability).contains(formFactor)
          ) {
            return State.empty();
          }
          if (station.isFloatingVehicle()) {
            if (!isVehicleAvailableDuringRentalPeriod(s0, station)) {
              return State.empty();
            }
            s1.beginFloatingVehicleRenting(formFactor, getPropulsionType(station), network, false);
          } else {
            boolean mayKeep =
              request.allowArrivingInRentedVehicleAtDestination() &&
              station.isArrivingInRentalVehicleAtDestinationAllowed();
            s1.beginVehicleRentingAtStation(
              formFactor,
              getPropulsionType(station),
              network,
              mayKeep,
              false
            );
          }
          pickedUp = true;
        }
        case HAVE_RENTED -> {
          return State.empty();
        }
        case RENTING_FLOATING, RENTING_FROM_STATION -> {
          if (!hasCompatibleNetworks(network, s0.getVehicleRentalNetwork())) {
            return State.empty();
          }
          if (!station.canDropOffFormFactor(formFactor, realtimeAvailability)) {
            return State.empty();
          }
          s1.dropOffRentedVehicleAtStation(formFactor, getPropulsionType(station), network, false);
          pickedUp = false;
        }
        default -> throw new IllegalStateException();
      }
    }

    s1.incrementWeight(
      pickedUp ? request.pickupCost().toSeconds() : request.dropOffCost().toSeconds()
    );
    s1.incrementTimeInMilliseconds(
      pickedUp ? request.pickupTime().toMillis() : request.dropOffTime().toMillis()
    );
    s1.setBackMode(null);
    return s1.makeStateArray();
  }

  private static boolean isVehicleAvailableDuringRentalPeriod(State s0, VehicleRentalPlace place) {
    if (s0.getRequest().rentalPeriod() != null && place.isCarStation()) {
      var vehicleRentalVehicle = (VehicleRentalVehicle) place;
      var availableUntil = vehicleRentalVehicle.availableUntil();
      if (availableUntil == null) {
        return true;
      }
      Instant rentalEndTime = s0.getRequest().rentalPeriod().end();
      return !availableUntil.isBefore(rentalEndTime);
    }
    return true;
  }

  @Override
  public I18NString getName() {
    return getToVertex().getName();
  }

  /**
   * @param stationNetwork The station network where we want to drop the bike off.
   * @param rentedNetwork  The networks of the station we rented the bike from.
   * @return true if the bike can be dropped off here, false if not.
   */
  private boolean hasCompatibleNetworks(String stationNetwork, @Nullable String rentedNetwork) {
    /*
     * Special case for "null" networks ("catch-all" network defined).
     */
    if (rentedNetwork == null) {
      return true;
    }

    return rentedNetwork.equals(stationNetwork);
  }

  private static boolean isFormFactorAllowed(StreetMode streetMode, RentalFormFactor formFactor) {
    return formFactor.traverseMode == StreetModeToRentalTraverseModeMapper.map(streetMode);
  }

  /**
   * Extract the propulsion type from the rental place.
   * For floating vehicles, this comes from the vehicle type.
   * For stations, we use the propulsion type of the first matching vehicle type,
   * defaulting to HUMAN if none is specified.
   */
  private PropulsionType getPropulsionType(VehicleRentalPlace place) {
    if (place instanceof VehicleRentalVehicle vehicle) {
      var vehicleType = vehicle.vehicleType();
      return vehicleType != null ? vehicleType.propulsionType() : PropulsionType.HUMAN;
    }
    if (place instanceof VehicleRentalStation station) {
      // For stations, find a matching vehicle type for this form factor
      return station
        .vehicleTypesAvailable()
        .keySet()
        .stream()
        .filter(vt -> vt.formFactor() == formFactor)
        .map(RentalVehicleType::propulsionType)
        .findFirst()
        .orElse(PropulsionType.HUMAN);
    }
    return PropulsionType.HUMAN;
  }
}
