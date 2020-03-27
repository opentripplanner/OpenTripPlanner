package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.BoardingArea;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

/** Responsible for mapping GTFS Boarding areas into the OTP model. */
class BoardingAreaMapper {
  private Map<org.onebusaway.gtfs.model.Stop, BoardingArea> mappedBoardingAreas = new HashMap<>();

  Collection<BoardingArea> map(Collection<org.onebusaway.gtfs.model.Stop> allBoardingAreas) {
    return MapUtils.mapToList(allBoardingAreas, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  BoardingArea map(org.onebusaway.gtfs.model.Stop orginal) {
    return orginal == null ? null : mappedBoardingAreas.computeIfAbsent(orginal, this::doMap);
  }

  private BoardingArea doMap(org.onebusaway.gtfs.model.Stop gtfsStop) {
    if (gtfsStop.getLocationType() != org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_BOARDING_AREA) {
      throw new IllegalArgumentException(
          "Expected type " + org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_BOARDING_AREA
              + ", but got " + gtfsStop.getLocationType());
    }

    BoardingArea otpBoardingArea = new BoardingArea();

    otpBoardingArea.setId(mapAgencyAndId(gtfsStop.getId()));
    otpBoardingArea.setName(gtfsStop.getName());
    if (gtfsStop.isLatSet()) {
      otpBoardingArea.setLat(gtfsStop.getLat());
    }
    if (gtfsStop.isLonSet()) {
      otpBoardingArea.setLon(gtfsStop.getLon());
    }
    otpBoardingArea.setCode(gtfsStop.getCode());
    otpBoardingArea.setDescription(gtfsStop.getDesc());
    otpBoardingArea.setUrl(gtfsStop.getUrl());
    otpBoardingArea.setWheelchairBoarding(WheelChairBoarding.valueOfGtfsCode(gtfsStop.getWheelchairBoarding()));
    var level = gtfsStop.getLevel();
    if (level != null) {
      otpBoardingArea.setLevelIndex(level.getIndex());
      otpBoardingArea.setLevelName(level.getName());
    }

    return otpBoardingArea;
  }
}
