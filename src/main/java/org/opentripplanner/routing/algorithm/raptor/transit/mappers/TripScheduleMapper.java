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
        final int numStops = tripTimes.getNumStops();
        Trip trip = tripTimes.trip;
        int serviceCode = tripTimes.serviceCode;

        int[] arrivals = new int[numStops];
        int[] departures = new int[numStops];

        for (int i = 0; i < numStops; i++) {
            arrivals[i] = tripTimes.getArrivalTime(i);
            departures[i] = tripTimes.getDepartureTime(i);
        }

        return new TripScheduleImpl(arrivals, departures, trip, tripPattern, serviceCode);
    }
}
