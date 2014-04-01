package org.opentripplanner.index.model;

import java.util.List;

import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.trippattern.TripTimes;

import com.beust.jcommander.internal.Lists;

public class TripTimeShort {

    public String stopId;
    public int scheduledDeparture;
    public int realtimeDeparture;
    public int departureDelay;
    public int scheduledArrival;
    public int realtimeArrival;
    public int arrivalDelay;
        
    public TripTimeShort(TripTimes tt, int i, Stop stop) {
        stopId = stop.getId().getId();
        scheduledArrival   = tt.getScheduledArrivalTime(i);
        scheduledDeparture = tt.getScheduledDepartureTime(i);
        realtimeArrival    = tt.getArrivalTime(i);
        realtimeDeparture  = tt.getDepartureTime(i);
        arrivalDelay   = tt.getArrivalDelay(i);
        departureDelay = tt.getDepartureDelay(i);
    }

    /**
     * must pass in both table and trip, because tripTimes do not have stops.
     */
    public static List<TripTimeShort> fromTripTimes (Timetable table, Trip trip) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));        
        List<TripTimeShort> out = Lists.newArrayList();
        for (int i = 0; i < times.getNumHops(); ++i) {
            out.add(new TripTimeShort(times, i, table.getPattern().getStop(i)));
        }
        return out;
    }
    
}
