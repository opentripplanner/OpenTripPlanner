package org.opentripplanner.street.model.edge;

import java.util.Set;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.street.model.vertex.VehicleRentalPlaceVertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

/**
 * Renting or dropping off a rented vehicle edge.
 *
 * @author laurent
 */
public class VehicleRentalEdge extends Edge {

  public FormFactor formFactor;

  public VehicleRentalEdge(VehicleRentalPlaceVertex vertex, FormFactor formFactor) {
    super(vertex, vertex);
    this.formFactor = formFactor;
  }

  public State traverse(State s0) {
    if (!s0.getRequest().mode().includesRenting()) {
      return null;
    }

    var allowedRentalFormFactors = allowedModes(s0.getRequest().mode());
    if (!allowedRentalFormFactors.isEmpty() && !allowedRentalFormFactors.contains(formFactor)) {
      return null;
    }

    StateEditor s1 = s0.edit(this);

    VehicleRentalPlaceVertex stationVertex = (VehicleRentalPlaceVertex) tov;
    VehicleRentalPlace station = stationVertex.getStation();
    String network = station.getNetwork();
    var preferences = s0.getPreferences();
    boolean realtimeAvailability = preferences.rental().useAvailabilityInformation();

    if (station.networkIsNotAllowed(s0.getRequest().rental())) {
      return null;
    }

    boolean pickedUp;
    if (s0.getRequest().arriveBy()) {
      switch (s0.getVehicleRentalState()) {
        case BEFORE_RENTING:
          return null;
        case HAVE_RENTED:
          if (
            realtimeAvailability &&
            (
              !station.allowDropoffNow() ||
              !station.getAvailableDropoffFormFactors(true).contains(formFactor)
            )
          ) {
            return null;
          }
          s1.dropOffRentedVehicleAtStation(formFactor, network, true);
          pickedUp = false;
          break;
        case RENTING_FLOATING:
          if (
            realtimeAvailability &&
            !station.getAvailablePickupFormFactors(true).contains(formFactor)
          ) {
            return null;
          }
          if (station.isFloatingVehicle()) {
            s1.beginFloatingVehicleRenting(formFactor, network, true);
            pickedUp = true;
          } else {
            return null;
          }
          break;
        case RENTING_FROM_STATION:
          if (
            realtimeAvailability &&
            (
              !station.allowPickupNow() ||
              !station.getAvailablePickupFormFactors(true).contains(formFactor)
            )
          ) {
            return null;
          }
          // For arriveBy searches mayKeepRentedVehicleAtDestination is only set in State#getInitialStates(),
          // and so here it is checked if this bicycle could have been kept at the destination
          if (
            s0.mayKeepRentedVehicleAtDestination() &&
            !station.isArrivingInRentalVehicleAtDestinationAllowed()
          ) {
            return null;
          }
          if (!hasCompatibleNetworks(network, s0.getVehicleRentalNetwork())) {
            return null;
          }
          s1.beginVehicleRentingAtStation(formFactor, network, false, true);
          pickedUp = true;
          break;
        default:
          throw new IllegalStateException();
      }
    } else {
      switch (s0.getVehicleRentalState()) {
        case BEFORE_RENTING:
          if (
            realtimeAvailability &&
            (
              !station.allowPickupNow() ||
              !station.getAvailablePickupFormFactors(true).contains(formFactor)
            )
          ) {
            return null;
          }
          if (station.isFloatingVehicle()) {
            s1.beginFloatingVehicleRenting(formFactor, network, false);
          } else {
            boolean mayKeep =
              s0.getRequest().rental().allowArrivingInRentedVehicleAtDestination() &&
              station.isArrivingInRentalVehicleAtDestinationAllowed();
            s1.beginVehicleRentingAtStation(formFactor, network, mayKeep, false);
          }
          pickedUp = true;
          break;
        case HAVE_RENTED:
          return null;
        case RENTING_FLOATING:
        case RENTING_FROM_STATION:
          if (!hasCompatibleNetworks(network, s0.getVehicleRentalNetwork())) {
            return null;
          }
          if (
            realtimeAvailability &&
            (
              !station.allowDropoffNow() ||
              !station.getAvailableDropoffFormFactors(true).contains(formFactor)
            )
          ) {
            return null;
          }
          if (
            !allowedRentalFormFactors.isEmpty() &&
            station
              .getAvailableDropoffFormFactors(realtimeAvailability)
              .stream()
              .noneMatch(allowedRentalFormFactors::contains)
          ) {
            return null;
          }
          s1.dropOffRentedVehicleAtStation(formFactor, network, false);
          pickedUp = false;
          break;
        default:
          throw new IllegalStateException();
      }
    }

    s1.incrementWeight(
      pickedUp ? preferences.rental().pickupCost() : preferences.rental().dropoffCost()
    );
    s1.incrementTimeInSeconds(
      pickedUp ? preferences.rental().pickupTime() : preferences.rental().dropoffTime()
    );
    s1.setBackMode(null);
    return s1.makeState();
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

  private static Set<FormFactor> allowedModes(StreetMode streetMode) {
    return switch (streetMode) {
      case BIKE_RENTAL -> Set.of(RentalVehicleType.FormFactor.BICYCLE);
      case SCOOTER_RENTAL -> Set.of(RentalVehicleType.FormFactor.SCOOTER);
      case CAR_RENTAL -> Set.of(RentalVehicleType.FormFactor.CAR);
      default -> Set.of();
    };
  }
}
