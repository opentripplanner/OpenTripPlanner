package org.opentripplanner.gtfs.mapping;

import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.opentripplanner.framework.collection.MapUtils;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.FareZone;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.RegularStopBuilder;
import org.opentripplanner.transit.model.site.Station;

/** Responsible for mapping GTFS Stop into the OTP model. */
class StopMapper {

  private final Map<org.onebusaway.gtfs.model.Stop, RegularStop> mappedStops = new HashMap<>();

  private final TranslationHelper translationHelper;
  private final Function<FeedScopedId, Station> stationLookUp;

  StopMapper(TranslationHelper translationHelper, Function<FeedScopedId, Station> stationLookUp) {
    this.translationHelper = translationHelper;
    this.stationLookUp = stationLookUp;
  }

  Collection<RegularStop> map(Collection<org.onebusaway.gtfs.model.Stop> allStops) {
    return MapUtils.mapToList(allStops, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  RegularStop map(org.onebusaway.gtfs.model.Stop orginal) {
    return orginal == null ? null : mappedStops.computeIfAbsent(orginal, this::doMap);
  }

  private RegularStop doMap(org.onebusaway.gtfs.model.Stop gtfsStop) {
    if (gtfsStop.getLocationType() != org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STOP) {
      throw new IllegalArgumentException(
        "Expected type " +
        org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STOP +
        ", but got " +
        gtfsStop.getLocationType()
      );
    }

    StopMappingWrapper base = new StopMappingWrapper(gtfsStop);
    RegularStopBuilder builder = RegularStop
      .of(base.getId())
      .withCode(base.getCode())
      .withCoordinate(base.getCoordinate())
      .withWheelchairAccessibility(base.getWheelchairAccessibility())
      .withLevel(base.getLevel())
      .withPlatformCode(gtfsStop.getPlatformCode())
      .withVehicleType(TransitModeMapper.mapMode(gtfsStop.getVehicleType()));

    builder.withName(
      translationHelper.getTranslation(
        org.onebusaway.gtfs.model.Stop.class,
        "name",
        base.getId().getId(),
        base.getName()
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

    builder.withUrl(
      translationHelper.getTranslation(
        org.onebusaway.gtfs.model.Stop.class,
        "url",
        base.getId().getId(),
        gtfsStop.getUrl()
      )
    );

    if (gtfsStop.getZoneId() != null) {
      builder.addFareZones(
        FareZone.of(new FeedScopedId(gtfsStop.getId().getAgencyId(), gtfsStop.getZoneId())).build()
      );
    }

    if (gtfsStop.getTimezone() != null) {
      builder.withTimeZone(ZoneId.of(gtfsStop.getTimezone()));
    }

    if (gtfsStop.getParentStation() != null) {
      builder.withParentStation(stationLookUp.apply(base.getParentStationId()));
    }

    return builder.build();
  }
}
