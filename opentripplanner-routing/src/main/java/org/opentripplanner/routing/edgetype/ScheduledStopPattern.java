package org.opentripplanner.routing.edgetype;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;

/**
 * A ScheduledStopPattern is an intermediate object used when processing GTFS files. It represents an ordered
 * list of stops and a service ID. Any two trips with the same stops in the same order, and that
 * operate on the same days, can be combined using a TripPattern to save memory.
 * 
 * (moved out of GTFSPatternHopFactory to be visible to the ArrayTripPattern constructor)
 */
public class ScheduledStopPattern {
    final ArrayList<Stop> stops;
    final ArrayList<Integer> pickups;
    final ArrayList<Integer> dropoffs;

    AgencyAndId calendarId;

    public ScheduledStopPattern(ArrayList<Stop> stops, ArrayList<Integer> pickups, ArrayList<Integer> dropoffs, AgencyAndId calendarId) {
        this.stops = stops;
        this.pickups = pickups;
        this.dropoffs = dropoffs;
        this.calendarId = calendarId;
    }

    public boolean equals(Object other) {
        if (other instanceof ScheduledStopPattern) {
            ScheduledStopPattern pattern = (ScheduledStopPattern) other;
            return pattern.stops.equals(stops) && pattern.calendarId.equals(calendarId) && pattern.pickups.equals(pickups) && pattern.dropoffs.equals(dropoffs);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.stops.hashCode() ^ this.calendarId.hashCode() + this.pickups.hashCode() + this.dropoffs.hashCode();
    }

    public String toString() {
        return "StopPattern(" + stops + ", " + calendarId + ")";
    }
    
    public static ScheduledStopPattern fromTrip(Trip trip, List<StopTime> stopTimes) {
        ArrayList<Stop> stops = new ArrayList<Stop>();
        ArrayList<Integer> pickups = new ArrayList<Integer>();
        ArrayList<Integer> dropoffs = new ArrayList<Integer>();
        for (StopTime stoptime : stopTimes) {
            stops.add(stoptime.getStop());
            pickups.add(stoptime.getPickupType());
            dropoffs.add(stoptime.getDropOffType());
        }
        return new ScheduledStopPattern(stops, pickups, dropoffs, trip.getServiceId());
    }

}
