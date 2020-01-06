package org.opentripplanner.api.model;

import org.opentripplanner.api.adapters.AgencyAndIdAdapter;
import org.opentripplanner.model.FeedScopedId;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


public class TripTimeShort {

    public static final int UNDEFINED = -1;
    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
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
    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    public FeedScopedId tripId;
    public String blockId;
    public String headsign;

    public TripTimeShort(org.opentripplanner.index.model.TripTimeShort other) {
        stopId             = other.stopId;
        stopIndex          = other.stopIndex;
        stopCount          = other.stopCount;
        scheduledArrival   = other.scheduledArrival;
        realtimeArrival    = other.realtimeArrival;
        arrivalDelay       = other.arrivalDelay;
        scheduledDeparture = other.scheduledDeparture;
        realtimeDeparture  = other.realtimeDeparture;
        departureDelay     = other.departureDelay;
        timepoint          = other.timepoint;
        realtime           = other.realtime;
        realtimeState      = RealTimeState.RealTimeState(other.realtimeState);
        blockId            = other.blockId;
        headsign           = other.headsign;
    }
}
