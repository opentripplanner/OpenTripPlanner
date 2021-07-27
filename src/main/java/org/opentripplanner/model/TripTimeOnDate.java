package org.opentripplanner.model;

import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a Trip at a specific stop index and on a specific service day. This is a read-only
 * data transfer object used to pass information from the OTP internal model to the APIs.
 */
public class TripTimeOnDate {

    public static final int UNDEFINED = -1;

    private final TripTimes tripTimes;
    private final int stopIndex;
    // This is only needed because TripTimes has no reference to TripPattern
    private final TripPattern tripPattern;
    private final ServiceDay serviceDay;

    /**
     * This is stop-specific, so the index i is a stop index, not a hop index.
     */
    public TripTimeOnDate(TripTimes tripTimes, int stopIndex, TripPattern tripPattern, ServiceDay serviceDay) {
        this.tripTimes = tripTimes;
        this.stopIndex = stopIndex;
        this.tripPattern = tripPattern;
        this.serviceDay = serviceDay;
    }

    /**
     * must pass in both table and trip, because tripTimes do not have stops.
     */
    public static List<TripTimeOnDate> fromTripTimes (Timetable table, Trip trip) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));        
        List<TripTimeOnDate> out = new ArrayList<>();
        // one per stop, not one per hop, thus the <= operator
        for (int i = 0; i < times.getNumStops(); ++i) {
            out.add(new TripTimeOnDate(times, i, table.getPattern(), null));
        }
        return out;
    }

    /**
     * must pass in both table and trip, because tripTimes do not have stops.
     * @param serviceDay service day to set, if null none is set
     */
    public static List<TripTimeOnDate> fromTripTimes(Timetable table, Trip trip,
            ServiceDay serviceDay) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));
        List<TripTimeOnDate> out = new ArrayList<>();
        // one per stop, not one per hop, thus the <= operator
        for (int i = 0; i < times.getNumStops(); ++i) {
            out.add(new TripTimeOnDate(times, i, table.getPattern(), serviceDay));
        }
        return out;
    }

    public static Comparator<TripTimeOnDate> compareByDeparture() {
        return Comparator.comparing(t -> t.getServiceDay() + t.getRealtimeDeparture());
    }

    public FeedScopedId getStopId() {
        return tripPattern.getStopPattern().getStop(stopIndex).getId();
    }

    public int getStopIndex() {
        return stopIndex;
    }

    public int getStopCount() {
        return tripTimes.getNumStops();
    }

    public int getScheduledArrival() {
        return tripTimes.getScheduledArrivalTime(stopIndex);
    }

    public int getScheduledDeparture() {
        return tripTimes.getScheduledDepartureTime(stopIndex);
    }

    public int getRealtimeArrival() {
        return isRealtime() && isCancelledStop()
            ? tripTimes.getScheduledArrivalTime(stopIndex) : tripTimes.getArrivalTime(stopIndex);
    }

    public int getRealtimeDeparture() {
        return isRealtime() && isCancelledStop()
        ? tripTimes.getScheduledDepartureTime(stopIndex) : tripTimes.getDepartureTime(stopIndex);
    }

    /**
     * Returns the actual arrival time if available. Otherwise -1 is returned.
     */
    public int getActualArrival() {
        return tripTimes.isRecordedStop(stopIndex) ? tripTimes.getArrivalTime(stopIndex) : UNDEFINED;
    }

    /**
     * Returns the actual departure time if available. Otherwise -1 is returned.
     */
    public int getActualDeparture() {
        return tripTimes.isRecordedStop(stopIndex) ? tripTimes.getDepartureTime(stopIndex) : UNDEFINED;
    }

    public int getArrivalDelay() {
        return tripTimes.getArrivalDelay(stopIndex);
    }

    public int getDepartureDelay() {
        return tripTimes.getDepartureDelay(stopIndex);
    }

    public boolean isTimepoint() {
        return tripTimes.isTimepoint(stopIndex);
    }

    public boolean isRealtime() {
        return !tripTimes.isScheduled();
    }

    public boolean isCancelledStop() {
        return tripPattern.getStopPattern().getPickup(stopIndex) == PickDrop.CANCELLED
            && tripPattern.getStopPattern().getDropoff(stopIndex) == PickDrop.CANCELLED;
    }

    /** Return {code true} if stop is cancelled, or trip is canceled/replaced */
    public boolean isCanceledEffectively() {
        return isCancelledStop() || tripTimes.getTrip().getTripAlteration().isCanceledOrReplaced();
    }

    public RealTimeState getRealtimeState() {
        return tripTimes.getRealTimeState();
    }

    public long getServiceDay() {
        return serviceDay != null ? serviceDay.time(0) : UNDEFINED;
    }

    public Trip getTrip() {
        return tripTimes.getTrip();
    }

    public String getBlockId() {
        return tripTimes.getTrip().getBlockId();
    }

    public String getHeadsign() {
        return tripTimes.getHeadsign(stopIndex);
    }

    public PickDrop getPickupType() {
        return tripPattern.getStopPattern().getPickup(stopIndex);
    }

    public PickDrop getDropoffType() {
        return tripPattern.getStopPattern().getDropoff(stopIndex);
    }

    public StopTimeKey getStopTimeKey() {
        return new StopTimeKey(tripTimes.getTrip().getId(), stopIndex);
    }
}
