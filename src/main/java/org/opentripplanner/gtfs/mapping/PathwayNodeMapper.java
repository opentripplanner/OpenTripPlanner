package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.model.PathwayNode;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.MapUtils;
import org.opentripplanner.util.TranslationHelper;

/** Responsible for mapping GTFS Node into the OTP model. */
class PathwayNodeMapper {

  static final String DEFAULT_NAME = "Pathway node";

  private final Map<org.onebusaway.gtfs.model.Stop, PathwayNode> mappedNodes = new HashMap<>();

  private final TranslationHelper translationHelper;

  PathwayNodeMapper(TranslationHelper translationHelper) {
    this.translationHelper = translationHelper;
  }

  Collection<PathwayNode> map(Collection<org.onebusaway.gtfs.model.Stop> allNodes) {
    return MapUtils.mapToList(allNodes, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  PathwayNode map(org.onebusaway.gtfs.model.Stop orginal) {
    return orginal == null ? null : mappedNodes.computeIfAbsent(orginal, this::doMap);
  }

  private PathwayNode doMap(org.onebusaway.gtfs.model.Stop gtfsStop) {
    if (gtfsStop.getLocationType() != org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_NODE) {
      throw new IllegalArgumentException(
        "Expected type " +
        org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_NODE +
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

    final I18NString description = translationHelper.getTranslation(
      org.onebusaway.gtfs.model.Stop.class,
      "desc",
      base.getId().getId(),
      Optional.ofNullable(base.getDescription()).orElse(DEFAULT_NAME)
    );

    return new PathwayNode(
      base.getId(),
      name,
      base.getCode(),
      description,
      base.getCoordinate(),
      base.getWheelchairBoarding(),
      base.getLevel()
    );
  }
}
