package org.opentripplanner.analyst.scenario;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * Remove trips from a scenario
 */
public class RemoveTrips extends TimetableFilter {

    @Override
    public String getType() {
        return "remove-trips";
    }

    @Override
    public TripTimes apply(Trip trip, TripTimes tt) {
        return matches(trip) ? null : tt;
    }

    @Override
    public FrequencyEntry apply(Trip trip, FrequencyEntry fe) {
        return matches(trip) ? null : fe;
    }
}
