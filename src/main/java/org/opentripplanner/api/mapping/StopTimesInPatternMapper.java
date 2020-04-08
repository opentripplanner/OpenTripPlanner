package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiStopTimesInPattern;
import org.opentripplanner.model.StopTimesInPattern;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class StopTimesInPatternMapper {

    public static List<ApiStopTimesInPattern> mapToApi(Collection<StopTimesInPattern> domain) {
        if(domain == null) { return null; }
        return domain.stream().map(StopTimesInPatternMapper::mapToApi).collect(Collectors.toList());
    }


    public static ApiStopTimesInPattern mapToApi(StopTimesInPattern domain) {
        if(domain == null) { return null; }

        ApiStopTimesInPattern api = new ApiStopTimesInPattern();

        api.pattern = TripPatternMapper.mapToApiShort(domain.pattern);
        api.times = TripTimeMapper.mapToApi(domain.times);

        return api;
    }
}
