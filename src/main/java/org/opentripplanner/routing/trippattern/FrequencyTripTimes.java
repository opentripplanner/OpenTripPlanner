package org.opentripplanner.routing.trippattern;

import org.onebusaway.gtfs.model.Frequency;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;

import java.util.List;

/**
 * Like a tripTimes, but can represent multiple trips following the same template at regular intervals.
 */
public class FrequencyTripTimes extends TripTimes {

    final int startTime;
    final int endTime;
    final int headwaySecs;
    final boolean exactTimes;
    int timeOffset; // how many seconds to subtract. or maybe just shift the scheduled arrv/dep times.

    public FrequencyTripTimes(Trip trip, List<StopTime> stopTimes, Frequency freq) {
        super(trip, stopTimes);
        this.startTime   = freq.getStartTime();
        this.endTime     = freq.getEndTime();
        this.headwaySecs = freq.getHeadwaySecs();
        this.exactTimes  = freq.getExactTimes() != 0;
    }

    // funcs for: trip is running, trip is acceptable according to params.

    void getNextDeparture (int time) {

    }

}
