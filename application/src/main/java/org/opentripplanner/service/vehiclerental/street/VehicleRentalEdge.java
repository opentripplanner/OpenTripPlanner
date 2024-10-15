package org.opentripplanner.service.vehiclerental.street;

import java.util.Collections;
import java.util.Set;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.street.model.RentalFormFactor;
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

    var allowedRentalFormFactors = allowedModes(s0.getRequest().mode());
    if (!allowedRentalFormFactors.isEmpty() && !allowedRentalFormFactors.contains(formFactor)) {
      return State.empty();
    }

    StateEditor s1 = s0.edit(this);

    VehicleRentalPlaceVertex stationVertex = (VehicleRentalPlaceVertex) tov;
    VehicleRentalPlace station = stationVertex.getStation();
    String network = station.getNetwork();
    var preferences = s0.getPreferences().rental(formFactor.traverseMode);
    boolean realtimeAvailability = preferences.useAvailabilityInformation();

    if (station.networkIsNotAllowed(preferences)) {
      return State.empty();
    }

    boolean pickedUp;
    if (s0.getRequest().arriveBy()) {
      switch (s0.getVehicleRentalState()) {
        case BEFORE_RENTING -> {
          return State.empty();
        }
        case HAVE_RENTED -> {
          if (
            (realtimeAvailability && !station.allowDropoffNow()) ||
            !station.getAvailableDropoffFormFactors(realtimeAvailability).contains(formFactor)
          ) {
            return State.empty();
          }
          s1.dropOffRentedVehicleAtStation(formFactor, network, true);
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
            !station.getAvailablePickupFormFactors(realtimeAvailability).contains(formFactor)
          ) {
            return State.empty();
          }
          if (station.isFloatingVehicle()) {
            s1.beginFloatingVehicleRenting(formFactor, network, true);
            pickedUp = true;
          } else {
            return State.empty();
          }
        }
        case RENTING_FROM_STATION -> {
          if (
            (realtimeAvailability && !station.allowPickupNow()) ||
            !station.getAvailablePickupFormFactors(realtimeAvailability).contains(formFactor)
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
          s1.beginVehicleRentingAtStation(formFactor, network, false, true);
          pickedUp = true;
        }
        default -> throw new IllegalStateException();
      }
    } else {
      switch (s0.getVehicleRentalState()) {
        case BEFORE_RENTING -> {
          if (
            (realtimeAvailability && !station.allowPickupNow()) ||
            !station.getAvailablePickupFormFactors(realtimeAvailability).contains(formFactor)
          ) {
            return State.empty();
          }
          if (station.isFloatingVehicle()) {
            s1.beginFloatingVehicleRenting(formFactor, network, false);
          } else {
            boolean mayKeep =
              preferences.allowArrivingInRentedVehicleAtDestination() &&
              station.isArrivingInRentalVehicleAtDestinationAllowed();
            s1.beginVehicleRentingAtStation(formFactor, network, mayKeep, false);
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
          var formFactors = station.getAvailableDropoffFormFactors(realtimeAvailability);
          if (
            (realtimeAvailability && !station.allowDropoffNow()) ||
            !formFactors.contains(formFactor)
          ) {
            return State.empty();
          }
          if (
            !allowedRentalFormFactors.isEmpty() &&
            Collections.disjoint(allowedRentalFormFactors, formFactors)
          ) {
            return State.empty();
          }
          s1.dropOffRentedVehicleAtStation(formFactor, network, false);
          pickedUp = false;
        }
        default -> throw new IllegalStateException();
      }
    }

    s1.incrementWeight(
      pickedUp ? preferences.pickupCost().toSeconds() : preferences.dropOffCost().toSeconds()
    );
    s1.incrementTimeInSeconds(
      pickedUp
        ? (int) preferences.pickupTime().toSeconds()
        : (int) preferences.dropOffTime().toSeconds()
    );
    s1.setBackMode(null);
    return s1.makeStateArray();
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
  private boolean hasCompatibleNetworks(String stationNetwork, String rentedNetwork) {
    /*
     * Special case for "null" networks ("catch-all" network defined).
     */
    if (rentedNetwork == null) {
      return true;
    }

    return rentedNetwork.equals(stationNetwork);
  }

  private static Set<RentalFormFactor> allowedModes(StreetMode streetMode) {
    return switch (streetMode) {
      case BIKE_RENTAL -> Set.of(RentalFormFactor.BICYCLE, RentalFormFactor.CARGO_BICYCLE);
      case SCOOTER_RENTAL -> Set.of(
        RentalFormFactor.SCOOTER,
        RentalFormFactor.SCOOTER_SEATED,
        RentalFormFactor.SCOOTER_STANDING
      );
      case CAR_RENTAL -> Set.of(RentalFormFactor.CAR);
      default -> Set.of();
    };
  }
}
