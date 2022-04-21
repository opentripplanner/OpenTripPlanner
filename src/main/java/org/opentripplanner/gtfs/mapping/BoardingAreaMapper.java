package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.model.BoardingArea;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.MapUtils;
import org.opentripplanner.util.TranslationHelper;

/**
 * Responsible for mapping GTFS Boarding areas into the OTP model.
 */
class BoardingAreaMapper {

  static final String DEFAULT_NAME = "Boarding area";

  private final Map<org.onebusaway.gtfs.model.Stop, BoardingArea> mappedBoardingAreas = new HashMap<>();

  private final TranslationHelper translationHelper;

  BoardingAreaMapper(TranslationHelper translationHelper) {
    this.translationHelper = translationHelper;
  }

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
        "Expected type " +
        org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_BOARDING_AREA +
        ", but got " +
        gtfsStop.getLocationType()
      );
    }

    StopMappingWrapper base = new StopMappingWrapper(gtfsStop);

    final I18NString name = translationHelper.getTranslation(
      org.onebusaway.gtfs.model.Stop.class,
      "name",
      base.getId().getId(),
      Optional.ofNullable(base.getName()).orElse(DEFAULT_NAME)
    );

    return new BoardingArea(
      base.getId(),
      name,
      base.getCode(),
      base.getDescription(),
      base.getCoordinate(),
      base.getWheelchairBoarding(),
      base.getLevel()
    );
  }
}
