package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.FareZone;
import org.opentripplanner.model.modes.TransitModeService;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/** Responsible for mapping GTFS Stop into the OTP model. */
class StopMapper {

  private Map<org.onebusaway.gtfs.model.Stop, Stop> mappedStops = new HashMap<>();

  private final TransitModeService transitModeService;

  public StopMapper() {
    this.transitModeService = null;
  }

  public StopMapper(
      TransitModeService transitModeService
  ) {
    this.transitModeService = transitModeService;
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
          "Expected type " + org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STOP + ", but got "
              + gtfsStop.getLocationType());
    }

    StopMappingWrapper base = new StopMappingWrapper(gtfsStop);

    // Map single GTFS ZoneId to OTP TariffZone collection
    Collection<FareZone> fareZones = getTariffZones(gtfsStop.getZoneId(),
        gtfsStop.getId().getAgencyId()
    );

    return new Stop(base.getId(),
        base.getName(),
        base.getCode(),
        base.getDescription(),
        base.getCoordinate(),
        base.getWheelchairBoarding(),
        base.getLevel(),
        gtfsStop.getPlatformCode(), fareZones,
        gtfsStop.getUrl(),
        gtfsStop.getTimezone() == null ? null : TimeZone.getTimeZone(gtfsStop.getTimezone()),
        (
            TransitModeMapper.mapMode(gtfsStop.getVehicleType(), transitModeService)
        )
    );
  }

  private Collection<FareZone> getTariffZones(String zoneId, String agencyId) {
    return zoneId != null
        ? Collections.singletonList(new FareZone(new FeedScopedId(agencyId, zoneId), null))
        : Collections.emptyList();
  }
}
