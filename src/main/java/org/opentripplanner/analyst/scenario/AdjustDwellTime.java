package org.opentripplanner.analyst.scenario;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.Collection;

/**
 * Adjust the dwell times on matched trips.
 */
public class AdjustDwellTime extends TripFilter {
    public static final long serialVersionUID = 1L;

    /** Stops for which to set the dwell time */
    public Collection<String> stopId;

    /** new dwell time in seconds */
    public int dwellTime;

    @Override
    public TripTimes apply(Trip trip, TripPattern tp, TripTimes tt) {
        if (!matches(trip))
            return tt;

        if (tt.getNumStops() == 0)
            return tt;

        // convert trip times to marginals
        int[] dwellTimes = new int[tt.getNumStops()];
        int[] hopTimes = new int[tt.getNumStops() - 1];

        int startTime = tt.getArrivalTime(0);

        for (int i = 0; i < tt.getNumStops(); i++) {
            // adjust dwell time in place as we loop over the stops
            if (stopId == null || stopId.contains(tp.stopPattern.stops[i].getId().getId()))
                dwellTimes[i] = dwellTime;
            else
                dwellTimes[i] = tt.getDepartureTime(i) - tt.getArrivalTime(i);

            if (i < hopTimes.length)
                hopTimes[i] = tt.getArrivalTime(i + 1) - tt.getDepartureTime(i);
        }

        // make a new triptimes
        // Note that this copies the original times, not ones that have been modified by other modifications
        // (suppose someone set the dwell time at some stops to one value because they have offboard fare collection
        //  and at other stops to a different value because they don't - this exists, for example, in San Francisco's
        //  Muni Metro, with offboard fare collection in the subway and onboard fare collection when running as a
        //  streetcar)
        // However, this doesn't matter, because we've manually saved the modified times above.
        TripTimes ret = new TripTimes(tt);

        // Note: this requires us to use getArrivalTime not getScheduledArrivalTime when constructing the times
        // This also means that one should include real-time data in the analysis graphs at their own peril
        int cumulativeTime = startTime;
        for (int i = 0; i < dwellTimes.length; i++) {
            ret.updateArrivalTime(i, cumulativeTime);
            cumulativeTime += dwellTimes[i];
            ret.updateDepartureTime(i, cumulativeTime);

            if (i < hopTimes.length)
                cumulativeTime += hopTimes[i];
        }

        return ret;
    }

    @Override
    public FrequencyEntry apply(Trip trip, TripPattern tp, FrequencyEntry fe) {
        if (!matches(trip))
            return fe;

        TripTimes tt = apply(trip, tp, fe.tripTimes);

        return new FrequencyEntry(fe.startTime, fe.endTime, fe.headway, fe.exactTimes, tt);
    }

    @Override
    public String getType() {
        return "adjust-dwell-time";
    }
}
