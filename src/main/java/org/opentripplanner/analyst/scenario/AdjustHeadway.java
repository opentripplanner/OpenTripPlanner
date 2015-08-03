package org.opentripplanner.analyst.scenario;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adjust headways on a route.
 */
public class AdjustHeadway extends TripFilter {
    public static final long serialVersionUID = 1L;

    /** The new headway, in seconds */
    public int headway;

    private static final Logger LOG = LoggerFactory.getLogger(AdjustHeadway.class);

    @Override
    public TripTimes apply(Trip trip, TripPattern tp, TripTimes tt) {
        if (matches(trip))
            LOG.warn("Not performing requested headway adjustment on timetabled trip {}", trip);

        return tt;
    }

    @Override
    public FrequencyEntry apply(Trip trip, TripPattern tp, FrequencyEntry fe) {
        if (matches(trip)) {
            return new FrequencyEntry(fe.startTime, fe.endTime, headway, fe.exactTimes, fe.tripTimes);
        }
        else {
            return fe;
        }
    }

    @Override
    public String getType() {
        return "adjust-headway";
    }
}
