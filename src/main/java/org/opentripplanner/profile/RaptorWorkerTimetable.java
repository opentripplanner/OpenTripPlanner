package org.opentripplanner.profile;

import com.google.common.collect.Lists;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RaptorWorkerTimetable implements Serializable {

    // TODO put stop indexes in array here
    // TODO serialize using deltas and variable-width from Protobuf libs

    int nTrips, nStops;
    private int[][] timesPerTrip;

    private RaptorWorkerTimetable(int nTrips, int nStops) {
        this.nTrips = nTrips;
        this.nStops = nStops;
        timesPerTrip = new int[nTrips][];
    }

    /**
     * Return the trip index within the pattern of the soonest departure at the given stop number.
     */
    public int findDepartureAfter(int stop, int time) {
        for (int trip = 0; trip < timesPerTrip.length; trip++) {
            if (getDeparture(trip, stop) > time + 60) {
                return trip;
            }
        }
        return -1;
    }

    public int getArrival (int trip, int stop) {
        return timesPerTrip[trip][stop * 2];
    }

    public int getDeparture (int trip, int stop) {
        return timesPerTrip[trip][stop * 2 + 1];
    }

    /** This is a factory function rather than a constructor to avoid calling the super constructor for rejected patterns. */
    public static RaptorWorkerTimetable forPattern (Graph graph, TripPattern pattern, TimeWindow window) {

        // Filter down the trips to only those running during the window
        // This filtering can reduce number of trips and run time by 80 percent
        BitSet servicesRunning = window.servicesRunning;
        List<TripTimes> tripTimes = Lists.newArrayList();
        TT: for (TripTimes tt : pattern.scheduledTimetable.tripTimes) {
            if (servicesRunning.get(tt.serviceCode) &&
                    tt.getScheduledArrivalTime(0) < window.to &&
                    tt.getScheduledDepartureTime(tt.getNumStops() - 1) >= window.from) {
                tripTimes.add(tt);
            }
        }
        if (tripTimes.isEmpty()) {
            return null; // no trips active, don't bother storing a timetable
        }

        // Sort the trip times by their first arrival time
        Collections.sort(tripTimes, new Comparator<TripTimes>() {
            @Override
            public int compare(TripTimes tt1, TripTimes tt2) {
                return (tt1.getScheduledArrivalTime(0) - tt2.getScheduledArrivalTime(0));
            }
        });

        // Copy the times into the compacted table
        RaptorWorkerTimetable rwtt = new RaptorWorkerTimetable(tripTimes.size(), pattern.getStops().size() * 2);
        int t = 0;
        for (TripTimes tt : tripTimes) {
            int[] times = new int[rwtt.nStops];
            for (int s = 0; s < pattern.getStops().size(); s++) {
                int arrival = tt.getScheduledArrivalTime(s);
                int departure = tt.getScheduledDepartureTime(s);
                times[s * 2] = arrival;
                times[s * 2 + 1] = departure;
            }
            rwtt.timesPerTrip[t++] = times;
        }
        return rwtt;
    }

}
