package org.opentripplanner.analyst.scenario;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * Remove trips from a scenario
 */
public class RemoveTrip extends TripFilter {

    @Override
    public String getType() {
        return "remove-trip";
    }

    @Override
    public TripTimes apply(Trip trip, TripPattern tp, TripTimes tt) {
        return matches(trip) ? null : tt;
    }

    @Override
    public FrequencyEntry apply(Trip trip, TripPattern tp, FrequencyEntry fe) {
        return matches(trip) ? null : fe;
    }
}
