package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiPlace;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PlaceMapper {

    public static List<ApiPlace> mapStopArrivals(Collection<StopArrival> domain) {
        if(domain == null) { return null; }

        return domain.stream().map(PlaceMapper::mapStopArrival).collect(Collectors.toList());
    }

    public static ApiPlace mapStopArrival(StopArrival domain) {
        return mapPlace(domain.place, domain.arrival, domain.departure);
    }

    public static ApiPlace mapPlace(Place domain, Calendar arrival, Calendar departure) {
        if(domain == null) { return null; }

        ApiPlace api = new ApiPlace();

        api.name = domain.name;
        api.stopId = FeedScopedIdMapper.mapToApi(domain.stopId);
        api.stopCode = domain.stopCode;
        api.platformCode = domain.platformCode;
        if(domain.coordinate != null) {
            api.lon = domain.coordinate.longitude();
            api.lat = domain.coordinate.latitude();
        }
        api.arrival = arrival;
        api.departure = departure;
        api.orig = domain.orig;
        api.zoneId = domain.zoneId;
        api.stopIndex = domain.stopIndex;
        api.stopSequence = domain.stopSequence;
        api.vertexType = VertexTypeMapper.mapVertexType(domain.vertexType);
        api.bikeShareId = domain.bikeShareId;

        return api;
    }
}
