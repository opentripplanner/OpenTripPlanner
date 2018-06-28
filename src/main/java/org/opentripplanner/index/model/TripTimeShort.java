package org.opentripplanner.index.model;

import java.util.List;

import org.apache.lucene.util.PriorityQueue;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;

import com.beust.jcommander.internal.Lists;

public class TripTimeShort {

    public static final int UNDEFINED = -1;
    public AgencyAndId stopId;
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
    public AgencyAndId tripId;
    public String blockId;
    public String headsign;
    public String stopDesc;
    public String stopName;

    /**
     * This is stop-specific, so the index i is a stop index, not a hop index.
     */
    public TripTimeShort(TripTimes tt, int i, Stop stop) {
        stopId = stop.getId();
        stopDesc = stop.getDesc();
        stopName = stop.getName();
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
        realtimeState      = tt.getRealTimeState();
        blockId            = tt.trip.getBlockId();
        headsign           = tt.getHeadsign(i);
    }

    public TripTimeShort(TripTimes tt, int i, Stop stop, ServiceDay sd) {
        this(tt, i, stop, sd.time(0));
    }

    public TripTimeShort(TripTimes tt, int i, Stop stop, long sd) {
        this(tt, i, stop);
        tripId = tt.trip.getId();
        serviceDay = sd;
    }

    public static List<TripTimeShort> fromTripTimes (Timetable table, Trip trip){
        return fromTripTimes(table, trip, 0);
    }
    /**
     * must pass in both table and trip, because tripTimes do not have stops.
     */
    public static List<TripTimeShort> fromTripTimes (Timetable table, Trip trip, long serviceDay) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));        
        List<TripTimeShort> out = Lists.newArrayList();
        for (int i = 0; i < times.getNumStops(); ++i) {
            out.add(
                new TripTimeShort(
                    times, i, table.pattern.getStop(i), serviceDay
                )
            );
        }
        return out;
    }

    /**
     * return the timetable for all trips with similar pattern (and same route)
     * starting from the current trip.
     * does not take into account frequency trips (Transperth doesnt use it)
     */
    public static List<TimetableForTrip> fromTimetable (Timetable table, Trip currentTrip, int numberOfDepartures, ServiceDay sd) {

        int currentIndex = table.getTripIndex(currentTrip.getId());
        TripTimes currenTripTimes = table.getTripTimes(currentIndex);
        List<TimetableForTrip> out = Lists.newArrayList();
        PriorityQueue<TripTimes> pq = new PriorityQueue<TripTimes> (numberOfDepartures) {
            @Override
            protected boolean lessThan (TripTimes t1, TripTimes t2) {
                return t1.getDepartureTime(0) > t2.getDepartureTime(0);
            }
        };

        // linear scanning as we cant count on trip times being sorted
        for (int i = 0; i < table.tripTimes.size(); i++) {
            TripTimes tt = table.tripTimes.get(i);
            
            if (!sd.serviceRunning(tt.serviceCode)) continue;
            if (tt.getDepartureTime(0) != -1 &&
                tt.getDepartureTime(0) >= currenTripTimes.getDepartureTime(0)
            ) {
                pq.insertWithOverflow(tt);
            }
        }
        while (pq.size() != 0) {
            TripTimes tt = pq.pop();
            out.add(
                0,
                new TimetableForTrip(
                    table,
                    tt,
                    sd,
                    tt.trip == currentTrip ? true : false
                )
            );
        }
        return out;
    }
}
