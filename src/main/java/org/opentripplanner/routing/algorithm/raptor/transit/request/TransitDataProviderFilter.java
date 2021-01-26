package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.trippattern.TripTimes;

public interface TransitDataProviderFilter {

  <T extends TripPatternForDate> boolean tripPatternPredicate(T tripPatternForDate);

  <T extends TripTimes> boolean tripTimesPredicate(T tripTimes);
}
