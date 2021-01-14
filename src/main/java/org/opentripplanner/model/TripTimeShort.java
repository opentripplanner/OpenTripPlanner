package org.opentripplanner.model;

import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// TODO OTP2 - Convert all fields to get-methods and keep reference to TripTimes and let
//           - getters call the appropriate TripTimes method instead of coping the fields.
public class TripTimeShort {

    public static final int UNDEFINED = -1;
    public FeedScopedId stopId;
    public int stopIndex;
    public int stopCount;
    public int scheduledArrival = UNDEFINED ;
    public int scheduledDeparture = UNDEFINED ;
    public int realtimeArrival = UNDEFINED ;
    public int realtimeDeparture = UNDEFINED ;
    public int arrivalDelay = UNDEFINED ;
    public int departureDelay = UNDEFINED ;
    public boolean timepoint = false;
    public boolean realtime = false;
    public boolean isCancelledStop;
    public RealTimeState realtimeState = RealTimeState.SCHEDULED ;
    public long serviceDay;
    public FeedScopedId tripId;
    public String blockId;
    public String headsign;
    public int pickupType;
    public int dropoffType;

    /**
     * This is stop-specific, so the index i is a stop index, not a hop index.
     */
    public TripTimeShort(TripTimes tt, int i, Stop stop) {
        stopId             = stop.getId();
        stopIndex          = i;
        stopCount          = tt.getNumStops();
        scheduledArrival   = tt.getScheduledArrivalTime(i);
        realtimeArrival    = tt.getArrivalTime(i);
        arrivalDelay       = tt.getArrivalDelay(i);
        scheduledDeparture = tt.getScheduledDepartureTime(i);
        realtimeDeparture  = tt.getDepartureTime(i);
        departureDelay     = tt.getDepartureDelay(i);
        timepoint          = tt.isTimepoint(i);
        realtime           = !tt.isScheduled();
        isCancelledStop    = tt.isCancelledStop(i);

        if (realtime && isCancelledStop) {
            /*
             Trips/stops cancelled in realtime should not present "23:59:59, yesterday" as arrival-/departureTime
             Setting realtime arrival and departure to planned times
             */
            realtimeArrival = scheduledArrival;
            realtimeDeparture = scheduledDeparture;
            arrivalDelay = 0;
            departureDelay = 0;
        } else {
            realtimeArrival    = tt.getArrivalTime(i);
            arrivalDelay       = tt.getArrivalDelay(i);
            realtimeDeparture  = tt.getDepartureTime(i);
            departureDelay     = tt.getDepartureDelay(i);
        }

        realtimeState      = tt.getRealTimeState();
        blockId            = tt.trip.getBlockId();
        headsign           = tt.getHeadsign(i);
        pickupType         = tt.getPickupType(i);
        dropoffType        = tt.getDropoffType(i);
    }

    public TripTimeShort(TripTimes tt, int i, Stop stop, ServiceDay sd) {
        this(tt, i, stop);
        tripId = tt.trip.getId();
        serviceDay = sd.time(0);
    }

    /**
     * must pass in both table and trip, because tripTimes do not have stops.
     */
    public static List<TripTimeShort> fromTripTimes (Timetable table, Trip trip) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));        
        List<TripTimeShort> out = new ArrayList<>();
        // one per stop, not one per hop, thus the <= operator
        for (int i = 0; i < times.getNumStops(); ++i) {
            out.add(new TripTimeShort(times, i, table.pattern.getStop(i)));
        }
        return out;
    }

    /**
     * must pass in both table and trip, because tripTimes do not have stops.
     * @param serviceDay service day to set, if null none is set
     */
    public static List<TripTimeShort> fromTripTimes(Timetable table, Trip trip,
            ServiceDay serviceDay) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));
        List<TripTimeShort> out = new ArrayList<>();
        // one per stop, not one per hop, thus the <= operator
        for (int i = 0; i < times.getNumStops(); ++i) {
            out.add(new TripTimeShort(times, i, table.pattern.getStop(i), serviceDay));
        }
        return out;
    }

    public static Comparator<TripTimeShort> compareByDeparture() {
        return Comparator.comparing(t -> t.serviceDay + t.realtimeDeparture);
    }
}
