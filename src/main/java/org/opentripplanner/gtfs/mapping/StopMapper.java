package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.Stop;
import org.opentripplanner.model.WheelChairBoarding;
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
        Stop otpStop = new Stop();

        otpStop.setId(mapAgencyAndId(rhs.getId()));
        otpStop.setName(rhs.getName());
        otpStop.setLat(rhs.getLat());
        otpStop.setLon(rhs.getLon());
        otpStop.setCode(rhs.getCode());
        otpStop.setDescription(rhs.getDesc());
        otpStop.setZone(rhs.getZoneId());
        otpStop.setUrl(rhs.getUrl());
        otpStop.setWheelchairBoarding(mapGtfsWheelChairCode(rhs.getWheelchairBoarding()));

        return otpStop;
    }

    private WheelChairBoarding mapGtfsWheelChairCode(int wheelChairCode) {
        switch (wheelChairCode) {
            case 2:
                return WheelChairBoarding.NOT_POSSIBLE;
            case 1:
                return WheelChairBoarding.POSSIBLE;
            case 0:
            default:
                return WheelChairBoarding.NO_INFORMATION;
        }
    }
}
