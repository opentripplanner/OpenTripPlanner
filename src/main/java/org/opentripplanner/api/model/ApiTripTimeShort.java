package org.opentripplanner.api.model;

import org.opentripplanner.api.mapping.FeedScopedIdMapper;
import org.opentripplanner.model.FeedScopedId;


public class ApiTripTimeShort {
    public static final int UNDEFINED = -1;

    public String stopId;
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

    public ApiTripTimeShort(org.opentripplanner.index.model.TripTimeShort other) {
        stopId             = FeedScopedIdMapper.mapToApi(other.stopId);
        stopIndex          = other.stopIndex;
        stopCount          = other.stopCount;
        scheduledArrival   = other.scheduledArrival;
        scheduledDeparture = other.scheduledDeparture;
        realtimeArrival    = other.realtimeArrival;
        realtimeDeparture  = other.realtimeDeparture;
        arrivalDelay       = other.arrivalDelay;
        departureDelay     = other.departureDelay;
        timepoint          = other.timepoint;
        realtime           = other.realtime;
        realtimeState      = RealTimeState.RealTimeState(other.realtimeState);
        blockId            = other.blockId;
        headsign           = other.headsign;
    }
}
