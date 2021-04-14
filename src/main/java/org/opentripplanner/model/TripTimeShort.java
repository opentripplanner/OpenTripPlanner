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

    private final FeedScopedId stopId;
    private final int stopIndex;
    private final int stopCount;
    private final int scheduledArrival;
    private final int scheduledDeparture;
    private final int realtimeArrival;
    private final int realtimeDeparture;
    private final boolean recordedStop;
    private final int arrivalDelay;
    private final int departureDelay;
    private final boolean timepoint;
    private final boolean realtime;
    private final boolean cancelledStop;
    private final RealTimeState realtimeState;
    private final long serviceDay;
    private final Trip trip;
    private final String blockId;
    private final String headsign;
    private final int pickupType;
    private final int dropoffType;

    /**
     * This is stop-specific, so the index i is a stop index, not a hop index.
     */
    public TripTimeShort(TripTimes tt, int i, Stop stop, ServiceDay sd) {
        serviceDay         = sd != null ? sd.time(0) : UNDEFINED;
        trip               = tt.trip;
        stopId             = stop.getId();
        stopIndex          = i;
        stopCount          = tt.getNumStops();
        scheduledArrival   = tt.getScheduledArrivalTime(i);
        scheduledDeparture = tt.getScheduledDepartureTime(i);
        timepoint          = tt.isTimepoint(i);
        realtime           = !tt.isScheduled();
        cancelledStop = tt.isCancelledStop(i);
        recordedStop = tt.isRecordedStop(i);

        if (isRealtime() && isCancelledStop()) {
            /*
             Trips/stops cancelled in realtime should not present "23:59:59, yesterday" as arrival-/departureTime
             Setting realtime arrival and departure to planned times
             */
            realtimeArrival = tt.getScheduledArrivalTime(i);
            realtimeDeparture = tt.getScheduledDepartureTime(i);
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

    /**
     * must pass in both table and trip, because tripTimes do not have stops.
     */
    public static List<TripTimeShort> fromTripTimes (Timetable table, Trip trip) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));        
        List<TripTimeShort> out = new ArrayList<>();
        // one per stop, not one per hop, thus the <= operator
        for (int i = 0; i < times.getNumStops(); ++i) {
            out.add(new TripTimeShort(times, i, table.pattern.getStop(i), null));
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
        return Comparator.comparing(t -> t.getServiceDay() + t.getRealtimeDeparture());
    }

    public FeedScopedId getStopId() {
        return stopId;
    }

    public int getStopIndex() {
        return stopIndex;
    }

    public int getStopCount() {
        return stopCount;
    }

    public int getScheduledArrival() {
        return scheduledArrival;
    }

    public int getScheduledDeparture() {
        return scheduledDeparture;
    }

    public int getRealtimeArrival() {
        return realtimeArrival;
    }

    public int getRealtimeDeparture() {
        return realtimeDeparture;
    }

    /**
     * Returns the actual arrival time if available. Otherwise -1 is returned.
     */
    public int getActualArrival() {
        return recordedStop ? realtimeArrival : UNDEFINED;
    }

    /**
     * Returns the actual departure time if available. Otherwise -1 is returned.
     */
    public int getActualDeparture() {
        return recordedStop ? realtimeDeparture : UNDEFINED;
    }

    public int getArrivalDelay() {
        return arrivalDelay;
    }

    public int getDepartureDelay() {
        return departureDelay;
    }

    public boolean isTimepoint() {
        return timepoint;
    }

    public boolean isRealtime() {
        return realtime;
    }

    public boolean isCancelledStop() {
        return cancelledStop;
    }

    /** Return {code true} if stop is cancelled, or trip is canceled/replaced */
    public boolean isCanceledEffectively() {
        return cancelledStop || trip.getTripAlteration().isCanceledOrReplaced();
    }

    public RealTimeState getRealtimeState() {
        return realtimeState;
    }

    public long getServiceDay() {
        return serviceDay;
    }

    public Trip getTrip() {
        return trip;
    }

    public String getBlockId() {
        return blockId;
    }

    public String getHeadsign() {
        return headsign;
    }

    public int getPickupType() {
        return pickupType;
    }

    public int getDropoffType() {
        return dropoffType;
    }
}
