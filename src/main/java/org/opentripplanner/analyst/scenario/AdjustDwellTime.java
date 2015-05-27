package org.opentripplanner.analyst.scenario;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.Collection;

/**
 * Adjust the dwell times on matched trips.
 */
public class AdjustDwellTime extends TimetableFilter {
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

        int startTime = tt.getScheduledArrivalTime(0);

        for (int i = 0; i < tt.getNumStops(); i++) {
            // adjust dwell time in place as we loop over the stops
            if (stopId == null || stopId.contains(tp.getStop(i).getId().getId()))
                dwellTimes[i] = dwellTime;
            else
                dwellTimes[i] = tt.getScheduledDepartureTime(i) - tt.getScheduledArrivalTime(i);

            if (i < hopTimes.length)
                hopTimes[i] = tt.getScheduledArrivalTime(i + 1) - tt.getScheduledDepartureTime(i);
        }

        // make a new triptimes
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
