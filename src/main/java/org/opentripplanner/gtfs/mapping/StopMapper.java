package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.WgsCoordinate;
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

    /** Map from GTFS to OTP model, {@code null} safe. */
    Stop map(org.onebusaway.gtfs.model.Stop orginal) {
        return orginal == null ? null : mappedStops.computeIfAbsent(orginal, this::doMap);
    }

    private Stop doMap(org.onebusaway.gtfs.model.Stop gtfsStop) {
        Stop otpStop = new Stop();

        otpStop.setId(mapAgencyAndId(gtfsStop.getId()));
        otpStop.setName(gtfsStop.getName());
        if (gtfsStop.isLatSet() && gtfsStop.isLonSet()) {
          otpStop.setCoordinate(new WgsCoordinate(gtfsStop.getLat(), gtfsStop.getLon()));
        }
        otpStop.setCode(gtfsStop.getCode());
        otpStop.setDescription(gtfsStop.getDesc());
        otpStop.setZone(gtfsStop.getZoneId());
        otpStop.setUrl(gtfsStop.getUrl());
        otpStop.setWheelchairBoarding(WheelChairBoarding.valueOfGtfsCode(gtfsStop.getWheelchairBoarding()));

        return otpStop;
    }
}
