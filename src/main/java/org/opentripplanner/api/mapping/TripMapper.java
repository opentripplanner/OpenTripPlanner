package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiTrip;
import org.opentripplanner.api.model.ApiTripShort;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Trip;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class TripMapper {
    public static ApiTrip mapToApi(Trip obj) {
        if(obj == null) { return null; }

        ApiTrip api = new ApiTrip();
        api.id = FeedScopedIdMapper.mapToApi(obj.getId());
        api.routeId = FeedScopedIdMapper.mapIdToApi(obj.getRoute());
        api.serviceId = FeedScopedIdMapper.mapToApi(obj.getServiceId());
        api.tripShortName = obj.getTripShortName();
        api.tripHeadsign = obj.getTripHeadsign();
        api.routeShortName = obj.getRouteShortName();
        api.directionId = obj.getDirectionId();
        api.blockId = obj.getBlockId();
        api.shapeId = FeedScopedIdMapper.mapToApi(obj.getShapeId());
        api.wheelchairAccessible = obj.getWheelchairAccessible();
        api.bikesAllowed = obj.getBikesAllowed();
        api.fareId = obj.getFareId();

        return api;
    }


    public static ApiTripShort mapToApiShort(Trip domain) {
        if(domain == null) { return null; }

        ApiTripShort api = new ApiTripShort();
        api.id = FeedScopedIdMapper.mapToApi(domain.getId());
        api.tripHeadsign = domain.getTripHeadsign();
        api.serviceId = FeedScopedIdMapper.mapToApi(domain.getServiceId());
        FeedScopedId shape = domain.getShapeId();

        // TODO OTP2 - All ids should be fully qualified including feed scope id.
        api.shapeId = shape == null ? null : shape.getId();
        api.direction = directionToApi(domain.getDirectionId());

        return api;
    }

    public static List<ApiTripShort> mapToApiShort(Collection<Trip> domain) {
        if(domain == null) { return null; }
        return domain.stream().map(TripMapper::mapToApiShort).collect(Collectors.toList());
    }


    private static Integer directionToApi(String directionId) {
        return directionId == null ? null : Integer.parseInt(directionId);
    }
}
