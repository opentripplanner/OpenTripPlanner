package org.opentripplanner.raptor.rangeraptor.multicriteria.ride;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.PatternRideView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;

/**
 * TODO C2 - Add JavaDoc
 */
public interface PatternRide<T extends RaptorTripSchedule>
  extends PatternRideView<T, McStopArrival<T>> {
  int tripSortIndex();
}
