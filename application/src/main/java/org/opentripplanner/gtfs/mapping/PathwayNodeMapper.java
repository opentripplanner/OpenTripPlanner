package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.PathwayNode;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.utils.collection.MapUtils;

/** Responsible for mapping GTFS Node into the OTP model. */
class PathwayNodeMapper {

  static final String DEFAULT_NAME = "Pathway node";

  private final IdFactory idFactory;
  private final Map<org.onebusaway.gtfs.model.Stop, PathwayNode> mappedNodes = new HashMap<>();

  private final TranslationHelper translationHelper;
  private final Function<FeedScopedId, Station> stationLookUp;

  PathwayNodeMapper(
    IdFactory idFactory,
    TranslationHelper translationHelper,
    Function<FeedScopedId, Station> stationLookUp
  ) {
    this.idFactory = idFactory;
    this.translationHelper = translationHelper;
    this.stationLookUp = stationLookUp;
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

    StopMappingWrapper base = new StopMappingWrapper(idFactory, gtfsStop);
    var builder = PathwayNode.of(base.getId())
      .withCode(base.getCode())
      .withCoordinate(base.getCoordinate())
      .withWheelchairAccessibility(base.getWheelchairAccessibility())
      .withLevel(base.getLevel());

    builder.withName(
      translationHelper.getTranslation(
        org.onebusaway.gtfs.model.Stop.class,
        "name",
        base.getId().getId(),
        Optional.ofNullable(base.getName()).orElse(DEFAULT_NAME)
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
