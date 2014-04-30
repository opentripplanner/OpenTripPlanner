package org.opentripplanner.index.model;

import java.util.List;

import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.trippattern.TripTimes;

import com.beust.jcommander.internal.Lists;

public class TripTimeShort {

    public static final int UNDEFINED = -1;
    public String stopId;
    public int scheduledArrival = UNDEFINED ;
    public int scheduledDeparture = UNDEFINED ;
    public int realtimeArrival = UNDEFINED ;
    public int realtimeDeparture = UNDEFINED ;
    public int arrivalDelay = UNDEFINED ;
    public int departureDelay = UNDEFINED ;

    /**
     * This is stop-specific, so the index i is a stop index, not a hop index.
     */
    public TripTimeShort(TripTimes tt, int i, Stop stop) {
        stopId = stop.getId().getId();
        if (i > 0) {
            scheduledArrival   = tt.getScheduledArrivalTime(i);
            realtimeArrival    = tt.getArrivalTime(i);
            arrivalDelay       = tt.getArrivalDelay(i);
            scheduledDeparture = tt.getScheduledDepartureTime(i);
            realtimeDeparture  = tt.getDepartureTime(i);
            departureDelay     = tt.getDepartureDelay(i);
        }
    }

    /**
     * must pass in both table and trip, because tripTimes do not have stops.
     */
    public static List<TripTimeShort> fromTripTimes (Timetable table, Trip trip) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));        
        List<TripTimeShort> out = Lists.newArrayList();
        // one per stop, not one per hop, thus the <= operator
        for (int i = 0; i < times.getNumStops(); ++i) {
            out.add(new TripTimeShort(times, i, table.getPattern().getStop(i)));
        }
        return out;
    }
    
}
