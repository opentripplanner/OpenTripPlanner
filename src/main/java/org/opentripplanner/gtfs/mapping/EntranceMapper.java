package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.Entrance;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

/** Responsible for mapping GTFS Entrance into the OTP model. */
class EntranceMapper {

  private Map<org.onebusaway.gtfs.model.Stop, Entrance> mappedEntrances = new HashMap<>();

  Collection<Entrance> map(Collection<org.onebusaway.gtfs.model.Stop> allEntrances) {
    return MapUtils.mapToList(allEntrances, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  Entrance map(org.onebusaway.gtfs.model.Stop orginal) {
    return orginal == null ? null : mappedEntrances.computeIfAbsent(orginal, this::doMap);
  }

  private Entrance doMap(org.onebusaway.gtfs.model.Stop gtfsStop) {
    Entrance otpEntrance = new Entrance();

    otpEntrance.setId(mapAgencyAndId(gtfsStop.getId()));
    otpEntrance.setName(gtfsStop.getName());
    otpEntrance.setCoordinate(new WgsCoordinate(gtfsStop.getLat(), gtfsStop.getLon()));
    otpEntrance.setCode(gtfsStop.getCode());
    otpEntrance.setDescription(gtfsStop.getDesc());
    otpEntrance.setUrl(gtfsStop.getUrl());
    otpEntrance.setWheelchairBoarding(
        WheelChairBoarding.valueOfGtfsCode(gtfsStop.getWheelchairBoarding())
    );

    return otpEntrance;
  }
}
