package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.Stop;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

/** Responsible for mapping GTFS Stop into the OTP model. */
class StopMapper {
    private Map<org.onebusaway.gtfs.model.Stop, Stop> mappedStops = new HashMap<>();

    Collection<Stop> map(Collection<org.onebusaway.gtfs.model.Stop> allStops) {
        return MapUtils.mapToList(allStops, this::map);
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */
    Stop map(org.onebusaway.gtfs.model.Stop orginal) {
        return orginal == null ? null : mappedStops.computeIfAbsent(orginal, this::doMap);
    }

    private Stop doMap(org.onebusaway.gtfs.model.Stop rhs) {
        Stop lhs = new Stop();

        lhs.setId(mapAgencyAndId(rhs.getId()));
        lhs.setName(rhs.getName());
        lhs.setLat(rhs.getLat());
        lhs.setLon(rhs.getLon());
        lhs.setCode(rhs.getCode());
        lhs.setDesc(rhs.getDesc());
        lhs.setZoneId(rhs.getZoneId());
        lhs.setUrl(rhs.getUrl());
        lhs.setLocationType(rhs.getLocationType());
        lhs.setParentStation(rhs.getParentStation());
        lhs.setWheelchairBoarding(rhs.getWheelchairBoarding());
        lhs.setDirection(rhs.getDirection());
        lhs.setTimezone(rhs.getTimezone());
        lhs.setVehicleType(rhs.getVehicleType());
        lhs.setPlatformCode(rhs.getPlatformCode());

        return lhs;
    }
}
