package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StationBuilder;
import org.opentripplanner.transit.model.site.StopTransferPriority;

/**
 * Responsible for mapping GTFS Stop into the OTP model.
 * <p>
 * NOTE! This class has state. This class also holds a index of all mapped stops to avoid mapping
 * the same stop twice. We do this because the library (onebusaway) return transfers with Stop
 * object references, not stop ids. Instead of looking up the Stops by id in the {@link
 * TransferMapper} we just use the this class to cache stops. This way, the order of which stops and
 * transfers are mapped does not matter.
 */
class StationMapper {

  /** @see StationMapper (this class JavaDoc) for way we need this. */
  private final Map<Stop, Station> mappedStops = new HashMap<>();

  private final TranslationHelper translationHelper;
  private final StopTransferPriority stationTransferPreference;

  StationMapper(
    TranslationHelper translationHelper,
    StopTransferPriority stationTransferPreference
  ) {
    this.translationHelper = translationHelper;
    this.stationTransferPreference = stationTransferPreference;
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  Station map(Stop orginal) {
    return orginal == null ? null : mappedStops.computeIfAbsent(orginal, this::doMap);
  }

  private Station doMap(Stop rhs) {
    if (rhs.getLocationType() != Stop.LOCATION_TYPE_STATION) {
      throw new IllegalArgumentException(
        "Expected location_type %s, but got %s for stops.txt entry %s".formatted(
            Stop.LOCATION_TYPE_STATION,
            rhs.getLocationType(),
            rhs
          )
      );
    }
    StationBuilder builder = Station.of(mapAgencyAndId(rhs.getId()))
      .withCoordinate(WgsCoordinateMapper.mapToDomain(rhs))
      .withCode(rhs.getCode());

    builder.withName(
      translationHelper.getTranslation(Stop.class, "name", rhs.getId().getId(), rhs.getName())
    );

    builder.withDescription(
      translationHelper.getTranslation(Stop.class, "desc", rhs.getId().getId(), rhs.getDesc())
    );

    builder.withUrl(
      translationHelper.getTranslation(Stop.class, "url", rhs.getId().getId(), rhs.getUrl())
    );

    builder.withPriority(stationTransferPreference);

    if (rhs.getTimezone() != null) {
      builder.withTimezone(ZoneId.of(rhs.getTimezone()));
    }
    return builder.build();
  }
}
