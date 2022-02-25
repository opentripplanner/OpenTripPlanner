package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.opentripplanner.model.FareZone;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.util.MapUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslationHelper;

/** Responsible for mapping GTFS Stop into the OTP model. */
class StopMapper {

  private Map<org.onebusaway.gtfs.model.Stop, Stop> mappedStops = new HashMap<>();

  Collection<Stop> map(Collection<org.onebusaway.gtfs.model.Stop> allStops) {
    return MapUtils.mapToList(allStops, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  Stop map(org.onebusaway.gtfs.model.Stop original) {
    return map(original, null);
  }

  Stop map(org.onebusaway.gtfs.model.Stop original, TranslationHelper translationHelper) {
    return original == null
            ? null
            : mappedStops.computeIfAbsent(original, k -> doMap(original, translationHelper));
  }

  private Stop doMap(org.onebusaway.gtfs.model.Stop gtfsStop, TranslationHelper translationHelper) {
    if (gtfsStop.getLocationType() != org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STOP) {
      throw new IllegalArgumentException(
          "Expected type " + org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STOP + ", but got "
              + gtfsStop.getLocationType());
    }

    StopMappingWrapper base = new StopMappingWrapper(gtfsStop);

    // Map single GTFS ZoneId to OTP TariffZone collection
    Collection<FareZone> fareZones = getTariffZones(gtfsStop.getZoneId(),
        gtfsStop.getId().getAgencyId()
    );

    if (translationHelper != null) {
      return new Stop(base.getId(),
              translationHelper.getTranslation(TranslationHelper.TABLE_STOPS,
                      TranslationHelper.STOP_NAME, base.getId().getId(),
                      null, base.getName()
              ),
              base.getCode(),
              base.getDescription(),
              base.getCoordinate(),
              base.getWheelchairBoarding(),
              base.getLevel(),
              gtfsStop.getPlatformCode(), fareZones,
              translationHelper.getTranslation(TranslationHelper.TABLE_STOPS,
                      TranslationHelper.STOP_URL, base.getId().getId(),
                      null, gtfsStop.getUrl()
              ),
              gtfsStop.getTimezone() == null ? null : TimeZone.getTimeZone(gtfsStop.getTimezone()),
              TransitModeMapper.mapMode(gtfsStop.getVehicleType()),
              null
      );
    }

    return new Stop(base.getId(),
        new NonLocalizedString(base.getName()),
        base.getCode(),
        base.getDescription(),
        base.getCoordinate(),
        base.getWheelchairBoarding(),
        base.getLevel(),
        gtfsStop.getPlatformCode(), fareZones,
        new NonLocalizedString(gtfsStop.getUrl()),
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
