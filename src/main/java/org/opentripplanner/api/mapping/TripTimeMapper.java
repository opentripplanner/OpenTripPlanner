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

        api.stopId             = FeedScopedIdMapper.mapToApi(domain.stopId);
        api.stopIndex          = domain.stopIndex;
        api.stopCount          = domain.stopCount;
        api.scheduledArrival   = domain.scheduledArrival;
        api.scheduledDeparture = domain.scheduledDeparture;
        api.realtimeArrival    = domain.realtimeArrival;
        api.realtimeDeparture  = domain.realtimeDeparture;
        api.arrivalDelay       = domain.arrivalDelay;
        api.departureDelay     = domain.departureDelay;
        api.timepoint          = domain.timepoint;
        api.realtime           = domain.realtime;
        api.realtimeState      = ApiRealTimeState.RealTimeState(domain.realtimeState);
        api.blockId            = domain.blockId;
        api.headsign           = domain.headsign;

        return api;
    }

}
