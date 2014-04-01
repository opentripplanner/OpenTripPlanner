package org.opentripplanner.routing.trippattern;

import org.onebusaway.gtfs.model.Frequency;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;

import java.util.List;

/**
 * Like a tripTimes, but can represent multiple trips following the same template at regular intervals.
 * Created by abyrd on 2014-03-31.
 */
public class FrequencyTripTimes extends TripTimes {

    Frequency freq;

    public FrequencyTripTimes(Trip trip, List<StopTime> stopTimes, Frequency freq) {
        super(trip, stopTimes);
        this.freq = freq;
    }

}
