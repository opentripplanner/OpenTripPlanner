package org.opentripplanner.ext.restapi.mapping;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.ext.restapi.model.ApiTrip;
import org.opentripplanner.ext.restapi.model.ApiTripShort;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;

public class TripMapper {

  public static ApiTrip mapToApi(Trip obj) {
    if (obj == null) {
      return null;
    }

    ApiTrip api = new ApiTrip();
    api.id = FeedScopedIdMapper.mapToApi(obj.getId());
    api.routeId = FeedScopedIdMapper.mapIdToApi(obj.getRoute());
    api.serviceId = FeedScopedIdMapper.mapToApi(obj.getServiceId());
    api.tripShortName = obj.getShortName();
    api.tripHeadsign = I18NStringMapper.mapToApi(obj.getHeadsign(), null);
    api.routeShortName = obj.getRoute().getShortName();
    final Integer directionId = DirectionMapper.mapToApi(obj.getDirection());
    if (directionId != null) {
      api.directionId = Integer.toString(directionId);
    }
    api.blockId = obj.getGtfsBlockId();
    api.shapeId = FeedScopedIdMapper.mapToApi(obj.getShapeId());
    api.wheelchairAccessible = WheelchairAccessibilityMapper.mapToApi(obj.getWheelchairBoarding());
    api.bikesAllowed = BikeAccessMapper.mapToApi(obj.getBikesAllowed());
    api.fareId = obj.getGtfsFareId();

    return api;
  }

  public static ApiTripShort mapToApiShort(Trip domain) {
    if (domain == null) {
      return null;
    }

    ApiTripShort api = new ApiTripShort();
    api.id = FeedScopedIdMapper.mapToApi(domain.getId());
    api.tripHeadsign = I18NStringMapper.mapToApi(domain.getHeadsign(), null);
    api.serviceId = FeedScopedIdMapper.mapToApi(domain.getServiceId());
    FeedScopedId shape = domain.getShapeId();

    // TODO OTP2 - All ids should be fully qualified including feed scope id.
    api.shapeId = shape == null ? null : shape.getId();
    api.direction = DirectionMapper.mapToApi(domain.getDirection());

    return api;
  }

  public static List<ApiTripShort> mapToApiShort(Stream<Trip> domain) {
    if (domain == null) {
      return null;
    }
    return domain.map(TripMapper::mapToApiShort).collect(Collectors.toList());
  }
}
