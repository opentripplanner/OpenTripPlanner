package org.opentripplanner.model;

import java.util.List;

import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;

/**
 * This class represents what is called a JourneyPattern in Transmodel: the sequence of stops at
 * which a trip (GTFS) or vehicle journey (Transmodel) calls, irrespective of the day on which 
 * service runs.
 * 
 * An important detail: Routes in GTFS are not a structurally important element, they just serve as
 * user-facing information. It is possible for the same journey pattern to appear in more than one
 * route. This is notmore than one route to contain
 * 
 * OTP already has several classes that represent this same thing: A TripPattern in the context of
 * routing. It represents all trips with the same stop pattern A ScheduledStopPattern in the GTFS
 * loading process. A RouteVariant in the TransitIndex, which has a unique human-readable name and
 * belongs to a particular route.
 * 
 * We would like to combine all these different classes into one.
 * 
 * Any two trips with the same stops in the same order, and that operate on the same days, can be
 * combined using a TripPattern to simplify the graph. This saves memory and reduces search
 * complexity since we only consider the trip that departs soonest for each pattern. AgencyAndId
 * calendarId has been removed. See issue #1320.
 */
public class StopPattern { // rename to StopPattern

    /* Constants for the GTFS pick up / drop off type fields. */
    // It would be nice to have an enum for these, but the equivalence with integers is important.
    public static final int PICKDROP_SCHEDULED = 0;
    public static final int PICKDROP_NONE = 1;
    public static final int PICKDROP_CALL_AGENCY = 2;
    public static final int PICKDROP_COORDINATE_WITH_DRIVER = 3;
    
    public final int size;
    public final Stop[] stops;
    public final int[]  pickups;
    public final int[]  dropoffs;

    public boolean equals(Object other) {
        if (other instanceof StopPattern) {
            StopPattern pattern = (StopPattern) other;
            return pattern.stops.equals(stops) && pattern.pickups.equals(pickups) && pattern.dropoffs.equals(dropoffs);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.stops.hashCode() + this.pickups.hashCode() + this.dropoffs.hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StopPattern: ");
        for (int i = 0, j = stops.length; i < j; ++i) {
            sb.append(String.format("%s [%d %d] ", stops[i].getCode(), pickups[i], dropoffs[i]));
        }
        return sb.toString();
    }

    private StopPattern (int size) {
        this.size = size;
        stops     = new Stop[size];
        pickups   = new int[size];
        dropoffs  = new int[size];
    }
    
    public StopPattern (List<StopTime> stopTimes) {
        this (stopTimes.size());
        for (int i = 0; i < size; ++i) {
            StopTime stopTime = stopTimes.get(i);
            stops[i] = stopTime.getStop();
            pickups[i] = stopTime.getPickupType();
            dropoffs[i] = stopTime.getDropOffType();
        }
    }

    
}
