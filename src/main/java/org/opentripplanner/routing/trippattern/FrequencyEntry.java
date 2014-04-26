package org.opentripplanner.routing.trippattern;

import org.onebusaway.gtfs.model.Frequency;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.MavenVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Like a tripTimes, but can represent multiple trips following the same template at regular intervals.
 */
public class FrequencyEntry extends TripTimes {

    private static final Logger LOG = LoggerFactory.getLogger(FrequencyEntry.class);
    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    final int startTime;
    final int endTime;
    final int headwaySecs;
    final boolean exactTimes;

    public FrequencyEntry(Trip trip, List<StopTime> stopTimes, Frequency freq) {
        super(trip, stopTimes);
        // TODO Shift the scheduled times to be relative to zero here.
        this.startTime   = freq.getStartTime();
        this.endTime     = freq.getEndTime();
        this.headwaySecs = freq.getHeadwaySecs();
        this.exactTimes  = freq.getExactTimes() != 0;
    }

    /*
        The TripTimes getDepartureTime / getArrivalTime methods do not care when the search is happening.
        The Frequency equivalents need to know when the search is happening, and need to be able to say
        no trip is possible. Therefore we need to add another specialized method.

        Fortunately all uses of the TripTimes itself in traversing edges use relative times,
        so we can fall back on the underlying TripTimes.
     */

    /**
     * Unlike the base TripTimes class, this one is sensitive to the search time t.
     */
    @Override
    public int nextDepartureTime (int hop, int t) {
        LOG.info("FreqTripTimes {} {} {}", getTrip().getRoute().toString(), startTime, endTime);
        if (t > endTime) return -1;
        // Start time and end time are for the first stop in the trip. Find the time offset for this stop.
        int stopOffset = getDepartureTime(hop) - getDepartureTime(0);
        int beg = startTime + stopOffset; // First time a vehicle passes by this stop.
        int end = endTime + stopOffset; // Latest a vehicle can pass by this stop.
        if (exactTimes) {
            for (int dep = beg; dep < end; dep += headwaySecs) {
                if (dep >= t) return dep;
            }
        } else {
            int dep = t + headwaySecs;
            if (dep < beg) return beg; // not quite right
            if (dep < end) return dep;
        }
        return -1;
    }

    /**
     * Unlike the base TripTimes class, this one is sensitive to the search time t.
     */
    @Override
    public int prevArrivalTime (int hop, int t) {
        if (t < startTime) return -1;
        int stopOffset = getArrivalTime(hop) - getDepartureTime(0);
        int beg = startTime + stopOffset; // First time a vehicle passes by this stop.
        int end = endTime + stopOffset; // Latest a vehicle can pass by this stop.
        if (exactTimes) {
            for (int dep = end; dep > beg; dep -= headwaySecs) {
                if (dep <= t) return dep;
            }
        } else {
            int dep = t - headwaySecs;
            if (dep > end) return end; // not quite right
            if (dep > beg) return dep;
        }
        return -1;
    }

}
