package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiRealTimeState;
import org.opentripplanner.api.model.ApiTripTimeShort;
import org.opentripplanner.model.TripTimeShort;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class TripTimeMapper {

    public static List<ApiTripTimeShort> mapToApi(Collection<TripTimeShort> domain) {
        if(domain == null) { return null; }
        return domain.stream().map(TripTimeMapper::mapToApi).collect(Collectors.toList());
    }


    public static ApiTripTimeShort mapToApi(TripTimeShort domain) {
        if(domain == null) { return null; }

        ApiTripTimeShort api = new ApiTripTimeShort();

        api.stopId             = FeedScopedIdMapper.mapToApi(domain.getStopId());
        api.stopIndex          = domain.getStopIndex();
        api.stopCount          = domain.getStopCount();
        api.scheduledArrival   = domain.getScheduledArrival();
        api.scheduledDeparture = domain.getScheduledDeparture();
        api.realtimeArrival    = domain.getRealtimeArrival();
        api.realtimeDeparture  = domain.getRealtimeDeparture();
        api.arrivalDelay       = domain.getArrivalDelay();
        api.departureDelay     = domain.getDepartureDelay();
        api.timepoint          = domain.isTimepoint();
        api.realtime           = domain.isRealtime();
        api.realtimeState      = ApiRealTimeState.RealTimeState(domain.getRealtimeState());
        api.blockId            = domain.getBlockId();
        api.headsign           = domain.getHeadsign();

        return api;
    }

}
