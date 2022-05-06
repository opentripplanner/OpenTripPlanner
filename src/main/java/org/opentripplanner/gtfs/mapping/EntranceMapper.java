package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.model.Entrance;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.MapUtils;
import org.opentripplanner.util.TranslationHelper;

/**
 * Responsible for mapping GTFS Entrance into the OTP model.
 */
class EntranceMapper {

  private final Map<org.onebusaway.gtfs.model.Stop, Entrance> mappedEntrances = new HashMap<>();

  private final TranslationHelper translationHelper;

  EntranceMapper(TranslationHelper translationHelper) {
    this.translationHelper = translationHelper;
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

    final I18NString name = translationHelper.getTranslation(
      org.onebusaway.gtfs.model.Stop.class,
      "name",
      base.getId().getId(),
      base.getName()
    );

    final I18NString desc = translationHelper.getTranslation(
      org.onebusaway.gtfs.model.Stop.class,
      "desc",
      base.getId().getId(),
      base.getDescription()
    );

    return new Entrance(
      base.getId(),
      name,
      base.getCode(),
      desc,
      base.getCoordinate(),
      base.getWheelchairBoarding(),
      base.getLevel()
    );
  }
}
