package org.opentripplanner.index.model;

import java.util.List;
import java.util.Objects;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;

import com.beust.jcommander.internal.Lists;

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
    public RealTimeState realtimeState = RealTimeState.SCHEDULED ;
    public long serviceDay;
    public FeedScopedId tripId;
    public String blockId;
    public String headsign;

    /**
     * This is stop-specific, so the index i is a stop index, not a hop index.
     */
    public TripTimeShort(TripTimes tt, int i, Stop stop) {
        stopId = stop.getId();
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
        tripId             = tt.trip.getId();
        realtimeState      = tt.isTimeCanceled(i) ? RealTimeState.CANCELED : tt.getRealTimeState();
        blockId            = tt.trip.getBlockId();
        headsign           = tt.getHeadsign(i);
    }

    public TripTimeShort(TripTimes tt, int i, Stop stop, ServiceDay sd) {
        this(tt, i, stop);
        if (sd != null) {
            serviceDay = sd.time(0);
        }
    }


    /**
     * must pass in both table and trip, because tripTimes do not have stops.
     */
    public static List<TripTimeShort> fromTripTimes(Timetable table, Trip trip) {
        return fromTripTimes(table, trip, null);
    }

    /**
     * must pass in both table and trip, because tripTimes do not have stops.
     * @param serviceDay service day to set, if null none is set
     */
    public static List<TripTimeShort> fromTripTimes(Timetable table, Trip trip,
        ServiceDay serviceDay) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));
        List<TripTimeShort> out = Lists.newArrayList();
        // one per stop, not one per hop, thus the <= operator
        for (int i = 0; i < times.getNumStops(); ++i) {
            out.add(new TripTimeShort(times, i, table.pattern.getStop(i), serviceDay));
        }
        return out;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(
            stopId,
            tripId,
            realtimeState,
            blockId,
            headsign,
            Integer.valueOf(stopIndex),
            Integer.valueOf(stopCount),
            Integer.valueOf(scheduledArrival),
            Integer.valueOf(realtimeArrival),
            Integer.valueOf(arrivalDelay),
            Integer.valueOf(scheduledDeparture),
            Integer.valueOf(realtimeDeparture),
            Integer.valueOf(departureDelay),
            Boolean.valueOf(timepoint),
            Boolean.valueOf(realtime),
            Long.valueOf(serviceDay)
        );
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TripTimeShort other = (TripTimeShort) obj;
        boolean result = stopIndex == other.stopIndex &&
                stopCount == other.stopCount &&
                scheduledArrival == other.scheduledArrival &&
                realtimeArrival == other.realtimeArrival &&
                arrivalDelay == other.arrivalDelay &&
                scheduledDeparture == other.scheduledDeparture &&
                realtimeDeparture == other.realtimeDeparture &&
                departureDelay == other.departureDelay &&
                timepoint == other.timepoint &&
                realtime == other.realtime &&
                serviceDay == other.serviceDay;
        if (stopId != null) {
            result &= stopId.equals(other.stopId);
        }
        if (tripId != null) {
            result &= tripId.equals(other.tripId);
        }
        if (realtimeState != null) {
            result &= realtimeState.equals(other.realtimeState);
        }
        if (blockId != null) {
            result &= blockId.equals(other.blockId);
        }
        if (headsign != null) {
            result &= headsign.equals(other.headsign);
        }
        return result;
    }
}
