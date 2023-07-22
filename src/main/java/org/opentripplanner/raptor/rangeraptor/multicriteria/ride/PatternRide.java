package org.opentripplanner.raptor.rangeraptor.multicriteria.ride;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.PatternRideView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;

/**
 * Interface for PatternRide used in Raptor. This interface extends the read-only
 * interface {@link PatternRideView} with methods used by Raptor witch may or may not
 * change the Ride(creating a copy).
 */
public interface PatternRide<T extends RaptorTripSchedule>
  extends PatternRideView<T, McStopArrival<T>> {
  /**
   * Change the ride by setting a new c2 value. Since, the ride is immutable the
   * new ride is copied and returned.
   */
  PatternRide<T> updateC2(int newC2);
}
