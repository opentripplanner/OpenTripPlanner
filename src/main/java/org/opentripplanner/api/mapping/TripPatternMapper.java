package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiPatternShort;
import org.opentripplanner.index.model.ApiPatternDetail;
import org.opentripplanner.index.model.ApiStopShort;
import org.opentripplanner.index.model.ApiTripShort;
import org.opentripplanner.model.TripPattern;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TripPatternMapper {

    public static List<ApiPatternShort> mapToApi(Collection<TripPattern> domainList) {
        if(domainList == null) {
            return null;
        }
        return domainList
                .stream()
                .map(TripPatternMapper::mapToApi)
                .collect(Collectors.toList());
    }

    public static ApiPatternShort mapToApi(TripPattern domain) {
        if(domain == null) {
            return null;
        }
        return mapBaseToApi(domain, ApiPatternShort::new);
    }

    public static ApiPatternDetail mapToApiDetailed(TripPattern domain) {
        if(domain == null) {
            return null;
        }

        ApiPatternDetail api = mapBaseToApi(domain, ApiPatternDetail::new);
        api.routeId = domain.route.getId();
        api.stops = ApiStopShort.list(domain.getStops());
        api.trips = ApiTripShort.list(domain.getTrips());
        return api;
    }

    private static <T extends ApiPatternShort> T mapBaseToApi(TripPattern domain, Supplier<T> create) {
        // TODO OTP2 - Refactor to use the pattern ID
        T api = create.get();
        api.id = domain.getCode();
        api.desc = domain.name;
        return api;
    }
}
