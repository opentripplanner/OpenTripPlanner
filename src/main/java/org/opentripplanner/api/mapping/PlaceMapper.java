package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiPlace;
import org.opentripplanner.model.plan.Place;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PlaceMapper {

    public static List<ApiPlace> mapPlaces(Collection<Place> domain) {
        if(domain == null) { return null; }
        return domain.stream().map(PlaceMapper::mapPlace).collect(Collectors.toList());
    }

    public static ApiPlace mapPlace(Place domain) {
        if(domain == null) { return null; }
        ApiPlace api = new ApiPlace();
        api.arrival = domain.arrival;
        api.name = domain.name;
        api.stopId = domain.stopId;
        api.stopCode = domain.stopCode;
        api.platformCode = domain.platformCode;
        api.lon = domain.lon;
        api.lat = domain.lat;
        api.departure = domain.departure;
        api.orig = domain.orig;
        api.zoneId = domain.zoneId;
        api.stopIndex = domain.stopIndex;
        api.stopSequence = domain.stopSequence;
        api.vertexType = VertexTypeMapper.mapVertexType(domain.vertexType);
        api.bikeShareId = domain.bikeShareId;
        return api;
    }
}
