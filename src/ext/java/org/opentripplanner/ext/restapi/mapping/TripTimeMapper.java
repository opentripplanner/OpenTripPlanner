package org.opentripplanner.ext.restapi.mapping;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.ext.restapi.model.ApiRealTimeState;
import org.opentripplanner.ext.restapi.model.ApiTripTimeShort;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.model.TripTimeOnDate;

public class TripTimeMapper {

  public static List<ApiTripTimeShort> mapToApi(Collection<TripTimeOnDate> domain) {
    if (domain == null) {
      return null;
    }
    return domain.stream().map(TripTimeMapper::mapToApi).collect(Collectors.toList());
  }

  public static ApiTripTimeShort mapToApi(TripTimeOnDate domain) {
    if (domain == null) {
      return null;
    }

    ApiTripTimeShort api = new ApiTripTimeShort();

    api.stopId = FeedScopedIdMapper.mapToApi(domain.getStop().getId());
    api.stopIndex = domain.getStopIndex();
    api.stopCount = domain.getStopCount();
    api.scheduledArrival = domain.getScheduledArrival();
    api.scheduledDeparture = domain.getScheduledDeparture();
    api.realtimeArrival = domain.getRealtimeArrival();
    api.realtimeDeparture = domain.getRealtimeDeparture();
    api.arrivalDelay = domain.getArrivalDelay();
    api.departureDelay = domain.getDepartureDelay();
    api.timepoint = domain.isTimepoint();
    api.realtime = domain.isRealtime();
    api.realtimeState = ApiRealTimeState.RealTimeState(domain.getRealTimeState());
    api.blockId = domain.getBlockId();
    api.headsign = I18NStringMapper.mapToApi(domain.getHeadsign(), null);
    api.tripId = FeedScopedIdMapper.mapToApi(domain.getTrip().getId());
    api.serviceDay = domain.getServiceDayMidnight();

    return api;
  }
}
