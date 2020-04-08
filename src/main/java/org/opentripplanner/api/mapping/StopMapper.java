package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiStop;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.WheelChairBoarding;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class StopMapper {

    public static List<ApiStop> mapToApi(Collection<Stop> domain) {
        if(domain == null) { return null; }
        return domain.stream().map(StopMapper::mapToApi).collect(Collectors.toList());
    }

    public static ApiStop mapToApi(Stop domain) {
        return mapToApi(domain, true);
    }

    public static ApiStop mapToApi(Stop domain, boolean extended) {
        if(domain == null) { return null; }

        ApiStop api = new ApiStop();
        api.id = FeedScopedIdMapper.mapToApi(domain.getId());
        api.lat = domain.getLat();
        api.lon = domain.getLon();
        api.code = domain.getCode();
        api.name = domain.getName();
        if (extended) {
            api.desc = domain.getDescription();
            api.zoneId = domain.getZone();
            api.url = domain.getUrl();
            api.locationType = 0;
            api.parentStation = domain.getParentStation().getId().getId();
            //api.stopTimezone = stop.getTimezone();
            api.wheelchairBoarding = domain.getWheelchairBoarding() == null
                    ? WheelChairBoarding.NO_INFORMATION.gtfsCode
                    : domain.getWheelchairBoarding().gtfsCode;
            //api.direction = stop.getDirection();
        }
        return api;
    }
}
