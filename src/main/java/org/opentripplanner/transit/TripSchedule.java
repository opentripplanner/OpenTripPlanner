package org.opentripplanner.transit;

import com.conveyal.gtfs.model.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 *
 */
public class TripSchedule implements Serializable, Comparable<TripSchedule> {

    private static final Logger LOG = LoggerFactory.getLogger(TripSchedule.class);

    String tripId;
    int[] arrivals;
    int[] departures;
    int flags;
    int serviceCode;
    TripSchedule nextInBlock = null;

    // Maybe make a TripSchedule.Factory so we don't have to pass in serviceCode or map.
    public TripSchedule(Trip trip, int[] arrivals, int[] departures, int serviceCode) {
        this.tripId = trip.trip_id;
        if (trip.bikes_allowed > 0) {
            setFlag(TripFlag.BICYCLE);
        }
        if (trip.wheelchair_accessible > 0) {
            setFlag(TripFlag.WHEELCHAIR);
        }
        this.arrivals = arrivals;
        this.departures = departures;
        this.serviceCode = serviceCode;
    }

    public void setFlag (TripFlag tripFlag) {
        flags |= tripFlag.flag;
    }

    public boolean getFlag (TripFlag tripFlag) {
        return (flags & tripFlag.flag) != 0;
    }

    @Override
    public int compareTo(TripSchedule other) {
        return this.departures[0] - other.departures[0];
    }


    /** @return whether it makes sense for the supplied trip to be served by the same vehicle as this trip. */
    public void chainTo (TripSchedule that) {
        // Check that chain is temporally coherent
        if (arrivals[arrivals.length - 1] <= that.departures[0]) {
            // FIXME need to resolve ambiguity around service dates + block IDs.
            // nextInBlock = that;
        } else {
            // FIXME this error is extremely common in Portland because block IDs are recycled across service days.
            LOG.debug("Trip {} arrives at terminus after the next trip in its block departs.", tripId);
        }
    }

}
