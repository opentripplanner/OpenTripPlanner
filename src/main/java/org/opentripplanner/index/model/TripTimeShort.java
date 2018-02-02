package org.opentripplanner.index.model;

import java.util.List;
import java.util.TimeZone;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;

import com.beust.jcommander.internal.Lists;

import static org.opentripplanner.util.DateUtils.formatDateIso;

public class TripTimeShort {

    public static final int UNDEFINED = -1;

    /** Stop of this arrival/departure */
    public AgencyAndId stopId;

    /** Index of this stop in the trip */
    public int stopIndex;

    /** Total number of stops in the trip */
    public int stopCount;

    /** scheduled arrival time, in seconds after midnight of the service day */
    public int scheduledArrival = UNDEFINED ;

    /** scheduled departure time, in seconds after midnight of the service day */
    public int scheduledDeparture = UNDEFINED ;

    /** realtime arrival time, in seconds after midnight of the service day, if realtime=true */
    public int realtimeArrival = UNDEFINED ;

    /** realtime departure time, in seconds after midnight of the service day, if realtime=true */
    public int realtimeDeparture = UNDEFINED ;

    /** realtime arrival delay on the trip, in seconds, if realtime=true */
    public int arrivalDelay = UNDEFINED ;

    /** realtime departure delay on the trip, in seconds, if realtime=true */
    public int departureDelay = UNDEFINED ;

    /** whether this stop is marked as a timepoint in GTFS */
    public boolean timepoint = false;

    /** true if there is realtime data for this arrival/departure; otherwise false. */
    public boolean realtime = false;

    /** If this arrival/departure comes from realtime, the relationship of the TripUpdate to static GTFS schedules */
    public RealTimeState realtimeState = RealTimeState.SCHEDULED ;

    /** service day, in UNIX epoch time */
    public long serviceDay;

    /** trip of arrival/departure */
    public AgencyAndId tripId;

    /** block of arrival/departure */
    public String blockId;

    /** Headsign associated with this trip. */
    public String tripHeadsign;

    /** arrival time in ISO-8601 format, in timezone of router. Realtime if available.*/
    public String arrivalFmt;

    /** departure time in ISO-8601 format, in timezone of router. Realtime if available.*/
    public String departureFmt;

    /** Headsign associated with this stop-time, if given in GTFS. */
    public String stopHeadsign;

    /** track number, if available */
    public String track;

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
        realtimeState      = tt.getRealTimeState();
        blockId            = tt.trip.getBlockId();
        tripHeadsign       = tt.trip.getTripHeadsign();
        stopHeadsign       = tt.hasStopHeadsigns() ? tt.getHeadsign(i) : null;
        track              = tt.getTrack(i);
    }

    public TripTimeShort(TripTimes tt, int i, Stop stop, ServiceDay sd, TimeZone tz) {
        this(tt, i, stop);
        tripId = tt.trip.getId();
        serviceDay = sd.time(0);
        arrivalFmt = formatDateIso(serviceDay + realtimeArrival, tz);
        departureFmt = formatDateIso(serviceDay + realtimeDeparture, tz);
    }

    /**
     * must pass in both table and trip, because tripTimes do not have stops.
     */
    public static List<TripTimeShort> fromTripTimes (Timetable table, Trip trip) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));        
        List<TripTimeShort> out = Lists.newArrayList();
        // one per stop, not one per hop, thus the <= operator
        for (int i = 0; i < times.getNumStops(); ++i) {
            out.add(new TripTimeShort(times, i, table.pattern.getStop(i)));
        }
        return out;
    }
}
