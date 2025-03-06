package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.utils.collection.MapUtils;

/**
 * Responsible for mapping GTFS Entrance into the OTP model.
 */
class EntranceMapper {

  private final Map<org.onebusaway.gtfs.model.Stop, Entrance> mappedEntrances = new HashMap<>();

  private final TranslationHelper translationHelper;
  private final Function<FeedScopedId, Station> stationLookUp;

  EntranceMapper(
    TranslationHelper translationHelper,
    Function<FeedScopedId, Station> stationLookUp
  ) {
    this.translationHelper = translationHelper;
    this.stationLookUp = stationLookUp;
  }

  Collection<Entrance> map(Collection<org.onebusaway.gtfs.model.Stop> allEntrances) {
    return MapUtils.mapToList(allEntrances, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  Entrance map(org.onebusaway.gtfs.model.Stop orginal) {
    return orginal == null ? null : mappedEntrances.computeIfAbsent(orginal, this::doMap);
  }

  private Entrance doMap(org.onebusaway.gtfs.model.Stop gtfsStop) {
    if (gtfsStop.getLocationType() != org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_ENTRANCE_EXIT) {
      throw new IllegalArgumentException(
        "Expected type " +
        org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_ENTRANCE_EXIT +
        ", but got " +
        gtfsStop.getLocationType()
      );
    }

    StopMappingWrapper base = new StopMappingWrapper(gtfsStop);

    var builder = Entrance.of(base.getId())
      .withCode(base.getCode())
      .withCoordinate(base.getCoordinate())
      .withWheelchairAccessibility(base.getWheelchairAccessibility())
      .withLevel(base.getLevel());

    builder.withName(
      translationHelper.getTranslation(
        org.onebusaway.gtfs.model.Stop.class,
        "name",
        base.getId().getId(),
        base.getName()
      )
    );

    builder = builder.withDescription(
      translationHelper.getTranslation(
        org.onebusaway.gtfs.model.Stop.class,
        "desc",
        base.getId().getId(),
        base.getDescription()
      )
    );

    if (gtfsStop.getParentStation() != null) {
      builder.withParentStation(stationLookUp.apply(base.getParentStationId()));
    }

    return builder.build();
  }
}
