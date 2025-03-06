package org.opentripplanner.gtfs.mapping;

import static org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_BOARDING_AREA;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.BoardingArea;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.utils.collection.MapUtils;

/**
 * Responsible for mapping GTFS Boarding areas into the OTP model.
 */
class BoardingAreaMapper {

  static final String DEFAULT_NAME = "Boarding area";

  private final Map<org.onebusaway.gtfs.model.Stop, BoardingArea> mappedBoardingAreas =
    new HashMap<>();

  private final TranslationHelper translationHelper;
  private final Function<FeedScopedId, RegularStop> stationLookUp;

  BoardingAreaMapper(
    TranslationHelper translationHelper,
    Function<FeedScopedId, RegularStop> stationLookUp
  ) {
    this.translationHelper = translationHelper;
    this.stationLookUp = stationLookUp;
  }

  Collection<BoardingArea> map(Collection<org.onebusaway.gtfs.model.Stop> allBoardingAreas) {
    return MapUtils.mapToList(allBoardingAreas, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  BoardingArea map(org.onebusaway.gtfs.model.Stop orginal) {
    return orginal == null ? null : mappedBoardingAreas.computeIfAbsent(orginal, this::doMap);
  }

  private BoardingArea doMap(org.onebusaway.gtfs.model.Stop gtfsStop) {
    if (gtfsStop.getLocationType() != LOCATION_TYPE_BOARDING_AREA) {
      throw new IllegalArgumentException(
        "Expected type " + LOCATION_TYPE_BOARDING_AREA + ", but got " + gtfsStop.getLocationType()
      );
    }

    StopMappingWrapper base = new StopMappingWrapper(gtfsStop);

    var builder = BoardingArea.of(base.getId())
      .withCode(base.getCode())
      .withCoordinate(base.getCoordinate())
      .withWheelchairAccessibility(base.getWheelchairAccessibility())
      .withLevel(base.getLevel());

    if (gtfsStop.getParentStation() != null) {
      // The BoardingArea is linked to the parent stop, not station
      builder.withParentStop(stationLookUp.apply(base.getParentStationId()));
    }

    builder.withName(
      translationHelper.getTranslation(
        org.onebusaway.gtfs.model.Stop.class,
        "name",
        base.getId().getId(),
        Optional.ofNullable(base.getName()).orElse(DEFAULT_NAME)
      )
    );

    builder.withDescription(
      translationHelper.getTranslation(
        org.onebusaway.gtfs.model.Stop.class,
        "desc",
        base.getId().getId(),
        base.getDescription()
      )
    );

    return builder.build();
  }
}
