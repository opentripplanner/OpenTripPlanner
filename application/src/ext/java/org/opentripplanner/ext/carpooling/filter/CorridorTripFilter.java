package org.opentripplanner.ext.carpooling.filter;

import javax.annotation.Nullable;
import org.opentripplanner.ext.carpooling.routing.CarpoolCorridor;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper.SnapResult;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Pre-filters carpool trips by whether their corridor can reach the snapped passenger at all: see
 * {@link CarpoolCorridor#mayServe}. A direct request needs both the pickup and the dropoff in
 * reach; an access or egress request only the passenger's own end.
 */
public class CorridorTripFilter implements CarpoolTripFilter {

  private final double maxCarSpeed;

  /** @param maxCarSpeed the fastest car speed in the street graph, in metres per second */
  public CorridorTripFilter(double maxCarSpeed) {
    this.maxCarSpeed = maxCarSpeed;
  }

  @Override
  public boolean isCandidateTrip(RoutableCarpoolTrip trip, SnappedPassenger passenger) {
    return reaches(trip, passenger.pickup()) && reaches(trip, passenger.dropoff());
  }

  private boolean reaches(RoutableCarpoolTrip trip, @Nullable SnapResult snap) {
    if (snap == null) {
      return true;
    }
    var point = new WgsCoordinate(snap.vertex().getCoordinate());
    return trip.corridor().mayServe(trip.vertices(), point, maxCarSpeed);
  }
}
