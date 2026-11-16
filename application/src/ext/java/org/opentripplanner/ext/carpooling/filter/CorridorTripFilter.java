package org.opentripplanner.ext.carpooling.filter;

import org.jspecify.annotations.Nullable;
import org.opentripplanner.ext.carpooling.routing.CarpoolCorridor;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper.SnapResult;

/**
 * Pre-filters carpool trips by whether their corridor can reach the snapped passenger at all: see
 * {@link CarpoolCorridor#mayServe}. A direct request needs both the pickup and the dropoff in
 * reach; an access or egress request only the passenger's own end.
 */
public class CorridorTripFilter implements CarpoolTripFilter {

  @Override
  public boolean isCandidateTrip(RoutableCarpoolTrip trip, SnappedPassenger passenger) {
    return reaches(trip, passenger.pickup()) && reaches(trip, passenger.dropoff());
  }

  private boolean reaches(RoutableCarpoolTrip trip, @Nullable SnapResult snap) {
    if (snap == null) {
      return true;
    }
    return trip.corridor().mayServe(snap.vertex());
  }
}
