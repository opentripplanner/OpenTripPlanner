package org.opentripplanner.transit;

import com.conveyal.gtfs.model.Trip;

import java.io.Serializable;

/**
 *
 */
public class TripSchedule implements Serializable {

    // TODO add Service ID checks! Here and in Patterns. Maybe check when flagging patterns.
    String tripId;
    int[] arrivals;
    int[] departures;
    int flags;
    int serviceCode;

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

}
