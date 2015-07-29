package org.opentripplanner.transit;

import com.conveyal.gtfs.model.Trip;

import java.io.Serializable;

/**
 *
 */
public class TripSchedule implements Serializable {

    String tripId;
    int[] arrivals;
    int[] departures;
    int flags;

    public TripSchedule(Trip trip, int[] arrivals, int[] departures) {
        this.tripId = trip.trip_id;
        if (trip.bikes_allowed > 0) {
            setFlag(TripFlag.BICYCLE);
        }
        if (trip.wheelchair_accessible > 0) {
            setFlag(TripFlag.WHEELCHAIR);
        }
        this.arrivals = arrivals;
        this.departures = departures;
    }

    public void setFlag (TripFlag tripFlag) {
        flags |= tripFlag.flag;
    }

    public boolean getFlag (TripFlag tripFlag) {
        return (flags & tripFlag.flag) != 0;
    }

}
