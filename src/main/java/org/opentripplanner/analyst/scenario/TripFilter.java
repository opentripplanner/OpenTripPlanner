package org.opentripplanner.analyst.scenario;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * A timetable filter that is applied to specific trips.
 */
public abstract class TripFilter extends TimetableFilter {
    /** Apply this modification to a Trip/ Do not modify the original trip times as they are part of the graph! */
    public abstract TripTimes apply (Trip trip, TripPattern tp, TripTimes tt);

    public abstract FrequencyEntry apply (Trip trip, TripPattern tp, FrequencyEntry fe);
}
