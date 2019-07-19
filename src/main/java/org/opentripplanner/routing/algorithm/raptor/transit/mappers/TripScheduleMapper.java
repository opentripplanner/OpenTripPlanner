package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.algorithm.raptor.transit.TripScheduleImpl;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;

class TripScheduleMapper {

    static TripScheduleImpl map(
            TripPattern tripPattern,
            TripTimes tripTimes
    ) {
        return new TripScheduleImpl(tripTimes, tripPattern);
    }
}
