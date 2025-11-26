package org.opentripplanner.gtfs.mapping;

import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.FareZone;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.RegularStopBuilder;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;
import org.opentripplanner.utils.collection.MapUtils;

/** Responsible for mapping GTFS Stop into the OTP model. */
class StopMapper {

  private final IdFactory idFactory;
  private final Map<org.onebusaway.gtfs.model.Stop, RegularStop> mappedStops = new HashMap<>();
  private final SiteRepositoryBuilder siteRepositoryBuilder;
  private final TranslationHelper translationHelper;
  private final Function<FeedScopedId, Station> stationLookUp;

  StopMapper(
    IdFactory idFactory,
    TranslationHelper translationHelper,
    Function<FeedScopedId, Station> stationLookUp,
    SiteRepositoryBuilder siteRepositoryBuilder
  ) {
    this.idFactory = idFactory;
    this.translationHelper = translationHelper;
    this.stationLookUp = stationLookUp;
    this.siteRepositoryBuilder = siteRepositoryBuilder;
  }

  Collection<RegularStop> map(Collection<org.onebusaway.gtfs.model.Stop> allStops) {
    return MapUtils.mapToList(allStops, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  RegularStop map(org.onebusaway.gtfs.model.Stop original) {
    return original == null ? null : mappedStops.computeIfAbsent(original, this::doMap);
  }

  private RegularStop doMap(org.onebusaway.gtfs.model.Stop gtfsStop) {
    TransitMode mode = TransitModeMapper.mapMode(gtfsStop.getVehicleType());

    assertLocationTypeIsStop(gtfsStop);
    StopMappingWrapper base = new StopMappingWrapper(idFactory, gtfsStop);
    RegularStopBuilder builder = siteRepositoryBuilder
      .regularStop(base.getId())
      .withCode(base.getCode())
      .withCoordinate(base.getCoordinate())
      .withWheelchairAccessibility(base.getWheelchairAccessibility())
      .withLevel(base.getLevel())
      .withPlatformCode(gtfsStop.getPlatformCode())
      .withVehicleType(mode)
      .withSometimesUsedRealtime(mapSometimesUsedRealtime(mode, gtfsStop));

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

  /**
   * If a stop is sometimes used by realtime, then we must include it in transfer generation, even
   * when it does not have any trips/routes visiting it.
   */
  private static boolean mapSometimesUsedRealtime(TransitMode mode, Stop gtfsStop) {
    if (OTPFeature.IncludeStopsUsedRealTimeInTransfers.isOn()) {
      if (mode == TransitMode.RAIL) {
        return true;
      }
      // We only consider rail and rail-replacement-bus stops when generating transfers. The reason
      // we do not include all stops is due to perfomance reasons. This should be replaced by
      // generating transfers as needed for realtime updates.
      else if (
        mode == TransitMode.BUS &&
        TransitModeMapper.isRailReplacementBusService(gtfsStop.getVehicleType())
      ) {
        return true;
      }
    }
    return false;
  }

  private void assertLocationTypeIsStop(Stop gtfsStop) {
    if (gtfsStop.getLocationType() != Stop.LOCATION_TYPE_STOP) {
      throw new IllegalArgumentException(
        "Expected location_type %s, but got %s for stops.txt entry %s".formatted(
            Stop.LOCATION_TYPE_STOP,
            gtfsStop.getLocationType(),
            gtfsStop
          )
      );
    }
  }
}
