package org.opentripplanner.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;

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
    private final Integer midnight;

    public TripTimeOnDate(TripTimes tripTimes, int stopIndex, TripPattern tripPattern, ServiceDay serviceDay) {
        this.tripTimes = tripTimes;
        this.stopIndex = stopIndex;
        this.tripPattern = tripPattern;
        this.midnight = serviceDay != null ? serviceDay.secondsSinceMidnight(0) : UNDEFINED;
    }

    public TripTimeOnDate(TripTimes tripTimes, int stopIndex, TripPattern tripPattern, Instant midnight) {
        this.tripTimes = tripTimes;
        this.stopIndex = stopIndex;
        this.tripPattern = tripPattern;
        this.midnight = (int) midnight.getEpochSecond();
    }

    /** Must pass in both Timetable and Trip, because TripTimes do not have a reference to StopPatterns. */
    public static List<TripTimeOnDate> fromTripTimes (Timetable table, Trip trip) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));        
        List<TripTimeOnDate> out = new ArrayList<>();
        for (int i = 0; i < times.getNumStops(); ++i) {
            out.add(new TripTimeOnDate(times, i, table.getPattern(), (ServiceDay) null));
        }
        return out;
    }

    /**
     * Must pass in both Timetable and Trip, because TripTimes do not have a reference to StopPatterns.
     * @param serviceDay service day to set, if null none is set
     */
    public static List<TripTimeOnDate> fromTripTimes(Timetable table, Trip trip,
            ServiceDay serviceDay) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));
        List<TripTimeOnDate> out = new ArrayList<>();
        for (int i = 0; i < times.getNumStops(); ++i) {
            out.add(new TripTimeOnDate(times, i, table.getPattern(), serviceDay));
        }
        return out;
    }

    public static Comparator<TripTimeOnDate> compareByDeparture() {
        return Comparator.comparing(t -> t.getServiceDay() + t.getRealtimeDeparture());
    }

    public FeedScopedId getStopId() {
        return tripPattern.getStop(stopIndex).getId();
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
        return tripTimes.isCancelledStop(stopIndex) ||
            tripPattern.isBoardAndAlightAt(stopIndex, PickDrop.CANCELLED);
    }

    /** Return {code true} if stop is cancelled, or trip is canceled/replaced */
    public boolean isCanceledEffectively() {
        return isCancelledStop()
            || tripTimes.isCanceled()
            || tripTimes.getTrip().getTripAlteration().isCanceledOrReplaced();
    }

    public RealTimeState getRealtimeState() {
        return tripTimes.getRealTimeState();
    }

    public long getServiceDay() {
        return midnight;
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

    public List<String> getHeadsignVias() {
        return tripTimes.getVia(stopIndex);
    }

    public PickDrop getPickupType() {
        return tripTimes.isCanceled() || tripTimes.isCancelledStop(stopIndex)
            ? PickDrop.CANCELLED
            : tripPattern.getBoardType(stopIndex);
    }

    public PickDrop getDropoffType() {
        return tripTimes.isCanceled() || tripTimes.isCancelledStop(stopIndex)
            ? PickDrop.CANCELLED
            : tripPattern.getAlightType(stopIndex);
    }

    public StopTimeKey getStopTimeKey() {
        return new StopTimeKey(tripTimes.getTrip().getId(), stopIndex);
    }

    public BookingInfo getPickupBookingInfo() {
        return tripTimes.getPickupBookingInfo(stopIndex);
    }

    public BookingInfo getDropOffBookingInfo() {
        return tripTimes.getDropOffBookingInfo(stopIndex);
    }
}
