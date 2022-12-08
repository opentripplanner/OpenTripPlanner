package org.opentripplanner.street.model.edge;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.tostring.ToStringBuilder;
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

  public StreetTransitEntityLink(StreetVertex fromv, T tov, Accessibility wheelchairAccessibility) {
    super(fromv, tov);
    this.transitEntityVertex = tov;
    this.wheelchairAccessibility = wheelchairAccessibility;
  }

  public StreetTransitEntityLink(T fromv, StreetVertex tov, Accessibility wheelchairAccessibility) {
    super(fromv, tov);
    this.transitEntityVertex = fromv;
    this.wheelchairAccessibility = wheelchairAccessibility;
  }

  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("from", fromv).addObj("to", tov).toString();
  }

  public boolean isRoundabout() {
    return false;
  }

  public State traverse(State s0) {
    // Forbid taking shortcuts composed of two street-transit links associated with the same stop in a row. Also
    // avoids spurious leg transitions. As noted in https://github.com/opentripplanner/OpenTripPlanner/issues/2815,
    // it is possible that two stops can have the same GPS coordinate thus creating a possibility for a
    // legitimate StreetTransitLink > StreetTransitLink sequence, so only forbid two StreetTransitLinks to be taken
    // if they are for the same stop.
    if (
      s0.backEdge instanceof StreetTransitEntityLink &&
      ((StreetTransitEntityLink<?>) s0.backEdge).transitEntityVertex == this.transitEntityVertex
    ) {
      return null;
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
        return null;
      } else if (wheelchairAccessibility == Accessibility.NO_INFORMATION) {
        s1.incrementWeight(accessibility.stop().unknownCost());
      } else if (wheelchairAccessibility == Accessibility.NOT_POSSIBLE) {
        s1.incrementWeight(accessibility.stop().inaccessibleCost());
      }
    }

    switch (s0.getNonTransitMode()) {
      case BICYCLE:
        // Forbid taking your own bike in the station if bike P+R activated.
        if (s0.getRequest().mode().includesParking() && !s0.isVehicleParked()) {
          return null;
        }
        // Forbid taking a (station) rental vehicle in the station. This allows taking along
        // floating rental vehicles.
        else if (
          s0.isRentingVehicleFromStation() &&
          !(
            s0.mayKeepRentedVehicleAtDestination() &&
            s0.getRequest().rental().allowArrivingInRentedVehicleAtDestination()
          )
        ) {
          return null;
        }
        // Allow taking an owned bike in the station
        break;
      case CAR:
        // Forbid taking your own car in the station if bike P+R activated.
        if (s0.getRequest().mode().includesParking() && !s0.isVehicleParked()) {
          return null;
        }
        // For Kiss & Ride allow dropping of the passenger before entering the station
        if (s0.getCarPickupState() != null) {
          if (canDropOffAfterDriving(s0) && isLeavingStreetNetwork(s0.getRequest().arriveBy())) {
            dropOffAfterDriving(s0, s1);
          } else {
            return null;
          }
        }
        // If Kiss & Ride (Taxi) mode is not enabled allow car traversal so that the Stop
        // may be reached by car
        break;
      case WALK:
        break;
      default:
        return null;
    }

    if (
      s0.isRentingVehicleFromStation() &&
      s0.mayKeepRentedVehicleAtDestination() &&
      s0.getRequest().rental().allowArrivingInRentedVehicleAtDestination()
    ) {
      s1.incrementWeight(pref.rental().arrivingInRentalVehicleAtDestinationCost());
    }

    s1.setBackMode(null);

    // streetToStopTime may be zero so that searching from the stop coordinates instead of
    // the stop id catch transit departing at that exact search time.
    int streetToStopTime = getStreetToStopTime();
    s1.incrementTimeInSeconds(streetToStopTime);
    s1.incrementWeight(STEL_TRAVERSE_COST + streetToStopTime);
    return s1.makeState();
  }

  public I18NString getName() {
    return this.transitEntityVertex.getName();
  }

  public LineString getGeometry() {
    Coordinate[] coordinates = new Coordinate[] { fromv.getCoordinate(), tov.getCoordinate() };
    return GeometryUtils.getGeometryFactory().createLineString(coordinates);
  }

  public double getDistanceMeters() {
    return 0;
  }

  protected abstract int getStreetToStopTime();

  protected T getTransitEntityVertex() {
    return transitEntityVertex;
  }

  boolean isLeavingStreetNetwork(boolean arriveBy) {
    return (arriveBy ? fromv : tov) == getTransitEntityVertex();
  }
}
