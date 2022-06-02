package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.opentripplanner.model.FareZone;
import org.opentripplanner.model.Stop;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.MapUtils;
import org.opentripplanner.util.TranslationHelper;

/** Responsible for mapping GTFS Stop into the OTP model. */
class StopMapper {

  private final Map<org.onebusaway.gtfs.model.Stop, Stop> mappedStops = new HashMap<>();

  private final TranslationHelper translationHelper;

  StopMapper(TranslationHelper translationHelper) {
    this.translationHelper = translationHelper;
  }

  Collection<Stop> map(Collection<org.onebusaway.gtfs.model.Stop> allStops) {
    return MapUtils.mapToList(allStops, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  Stop map(org.onebusaway.gtfs.model.Stop orginal) {
    return orginal == null ? null : mappedStops.computeIfAbsent(orginal, this::doMap);
  }

  private Stop doMap(org.onebusaway.gtfs.model.Stop gtfsStop) {
    if (gtfsStop.getLocationType() != org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STOP) {
      throw new IllegalArgumentException(
        "Expected type " +
        org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STOP +
        ", but got " +
        gtfsStop.getLocationType()
      );
    }

    StopMappingWrapper base = new StopMappingWrapper(gtfsStop);

    // Map single GTFS ZoneId to OTP TariffZone collection
    Collection<FareZone> fareZones = getTariffZones(
      gtfsStop.getZoneId(),
      gtfsStop.getId().getAgencyId()
    );

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

    I18NString url = null;

    if (gtfsStop.getUrl() != null) {
      url =
        translationHelper.getTranslation(
          org.onebusaway.gtfs.model.Stop.class,
          "url",
          base.getId().getId(),
          gtfsStop.getUrl()
        );
    }

    return new Stop(
      base.getId(),
      name,
      base.getCode(),
      desc,
      base.getCoordinate(),
      base.getWheelchairAccessibility(),
      base.getLevel(),
      gtfsStop.getPlatformCode(),
      fareZones,
      url,
      gtfsStop.getTimezone() == null ? null : TimeZone.getTimeZone(gtfsStop.getTimezone()),
      TransitModeMapper.mapMode(gtfsStop.getVehicleType()),
      null
    );
  }

  private Collection<FareZone> getTariffZones(String zoneId, String agencyId) {
    return zoneId != null
      ? Collections.singletonList(new FareZone(new FeedScopedId(agencyId, zoneId), null))
      : Collections.emptyList();
  }
}
