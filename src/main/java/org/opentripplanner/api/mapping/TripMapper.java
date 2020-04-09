package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiTrip;
import org.opentripplanner.model.Trip;

public class TripMapper {
    public static ApiTrip mapToApi(Trip obj) {
        if(obj == null) {
            return null;
        }

        ApiTrip api = new ApiTrip();
        api.id = FeedScopedIdMapper.mapToApi(obj.getId());
        api.routeId = FeedScopedIdMapper.mapToApi(obj.getRoute().getId());
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
}
