package org.opentripplanner.street.model.edge;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.transit.model.basic.Accessibility;

/**
 * This represents the connection between a street vertex and a transit vertex.
 */
public abstract class StreetTransitEntityLink<T extends Vertex>
  extends Edge
  implements CarPickupableEdge {

  static final int STEL_TRAVERSE_COST = 1;

  private final T transitEntityVertex;

  private final Accessibility wheelchairAccessibility;

  protected StreetTransitEntityLink(
    StreetVertex fromv,
    T tov,
    Accessibility wheelchairAccessibility
  ) {
    super(fromv, tov);
    this.transitEntityVertex = tov;
    this.wheelchairAccessibility = wheelchairAccessibility;
  }

  protected StreetTransitEntityLink(
    T fromv,
    StreetVertex tov,
    Accessibility wheelchairAccessibility
  ) {
    super(fromv, tov);
    this.transitEntityVertex = fromv;
    this.wheelchairAccessibility = wheelchairAccessibility;
  }

  @Override
  public State[] traverse(State s0) {
    // Forbid taking shortcuts composed of two street-transit links associated with the same stop in a row. Also
    // avoids spurious leg transitions. As noted in https://github.com/opentripplanner/OpenTripPlanner/issues/2815,
    // it is possible that two stops can have the same GPS coordinate thus creating a possibility for a
    // legitimate StreetTransitLink > StreetTransitLink sequence, so only forbid two StreetTransitLinks to be taken
    // if they are for the same stop.
    if (
      s0.backEdge instanceof StreetTransitEntityLink<?> link &&
      link.transitEntityVertex == this.transitEntityVertex
    ) {
      return State.empty();
    }

    RoutingPreferences pref = s0.getPreferences();

    // Do not check here whether any transit modes are selected. A check for the presence of
    // transit modes will instead be done in the following PreBoard edge.
    // This allows searching for nearby transit stops using walk-only options.
    StateEditor s1 = s0.edit(this);

    if (s0.getRequest().wheelchair()) {
      var accessibility = pref.wheelchair();
      if (
        accessibility.stop().onlyConsiderAccessible() &&
        wheelchairAccessibility != Accessibility.POSSIBLE
      ) {
        return State.empty();
      } else if (wheelchairAccessibility == Accessibility.NO_INFORMATION) {
        s1.incrementWeight(accessibility.stop().unknownCost());
      } else if (wheelchairAccessibility == Accessibility.NOT_POSSIBLE) {
        s1.incrementWeight(accessibility.stop().inaccessibleCost());
      }
    }

    return switch (s0.currentMode()) {
      case BICYCLE, SCOOTER -> {
        // Forbid taking your own bike in the station if bike P+R activated.
        if (s0.getRequest().mode().includesParking() && !s0.isVehicleParked()) {
          yield State.empty();
        }
        // Forbid taking a (station) rental vehicle in the station. This allows taking along
        // floating rental vehicles.
        else if (
          s0.isRentingVehicleFromStation() &&
          !(s0.mayKeepRentedVehicleAtDestination() &&
            s0
              .getRequest()
              .preferences()
              .rental(s0.getRequest().mode())
              .allowArrivingInRentedVehicleAtDestination())
        ) {
          yield State.empty();
        }
        yield buildState(s0, s1, pref);
      }
      // Allow taking an owned bike in the station
      case CAR -> {
        // Forbid taking your own car in the station if bike P+R activated.
        if (s0.getRequest().mode().includesParking() && !s0.isVehicleParked()) {
          yield State.empty();
        }
        // For Kiss & Ride allow dropping of the passenger before entering the station
        if (s0.getCarPickupState() != null) {
          if (canDropOffAfterDriving(s0) && isLeavingStreetNetwork(s0.getRequest().arriveBy())) {
            dropOffAfterDriving(s0, s1);
          } else {
            yield State.empty();
          }
        }
        if (s0.isRentingVehicleFromStation()) {
          yield State.empty();
        }
        yield buildState(s0, s1, pref);
      }
      // If Kiss & Ride (Taxi) mode is not enabled allow car traversal so that the Stop
      // may be reached by car
      case WALK -> buildState(s0, s1, pref);
      case FLEX -> State.empty();
    };
  }

  private State[] buildState(State s0, StateEditor s1, RoutingPreferences pref) {
    if (s0.isRentingVehicleFromStation() && s0.mayKeepRentedVehicleAtDestination()) {
      var rentalPreferences = s0.getRequest().preferences().rental(s0.getRequest().mode());
      if (rentalPreferences.allowArrivingInRentedVehicleAtDestination()) {
        s1.incrementWeight(
          rentalPreferences.arrivingInRentalVehicleAtDestinationCost().toSeconds()
        );
      }
    }

    s1.setBackMode(null);

    // streetToStopTime may be zero so that searching from the stop coordinates instead of
    // the stop id catch transit departing at that exact search time.
    int streetToStopTime = getStreetToStopTime();
    s1.incrementTimeInSeconds(streetToStopTime);
    s1.incrementWeight(STEL_TRAVERSE_COST + streetToStopTime);
    return s1.makeStateArray();
  }

  public I18NString getName() {
    return this.transitEntityVertex.getName();
  }

  public LineString getGeometry() {
    Coordinate[] coordinates = new Coordinate[] { fromv.getCoordinate(), tov.getCoordinate() };
    return GeometryUtils.getGeometryFactory().createLineString(coordinates);
  }

  protected abstract int getStreetToStopTime();

  protected T getTransitEntityVertex() {
    return transitEntityVertex;
  }

  boolean isLeavingStreetNetwork(boolean arriveBy) {
    return (arriveBy ? fromv : tov) == getTransitEntityVertex();
  }
}
