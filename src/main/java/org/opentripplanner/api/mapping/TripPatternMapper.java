package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiPatternShort;
import org.opentripplanner.api.model.ApiPatternDetail;
import org.opentripplanner.model.TripPattern;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TripPatternMapper {

    public static List<ApiPatternShort> mapToApiShort(Collection<TripPattern> domainList) {
        if(domainList == null) {
            return null;
        }
        return domainList
                .stream()
                .map(TripPatternMapper::mapToApiShort)
                .collect(Collectors.toList());
    }

    public static ApiPatternShort mapToApiShort(TripPattern domain) {
        if(domain == null) {
            return null;
        }
        return mapToApiShort(domain, ApiPatternShort::new);
    }

    public static ApiPatternDetail mapToApiDetailed(TripPattern domain) {
        if(domain == null) {
            return null;
        }

        ApiPatternDetail api = mapToApiShort(domain, ApiPatternDetail::new);
        api.stops = StopMapper.mapToApiShort(domain.getStops());
        api.trips = TripMapper.mapToApiShort(domain.getTrips());
        return api;
    }

    private static <T extends ApiPatternShort> T mapToApiShort(TripPattern domain, Supplier<T> create) {
        T api = create.get();
        api.id = FeedScopedIdMapper.mapToApi(domain.getId());
        api.desc = domain.name;
        api.routeId = FeedScopedIdMapper.mapIdToApi(domain.route);
        return api;
    }
}
