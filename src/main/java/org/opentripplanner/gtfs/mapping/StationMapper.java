package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.opentripplanner.model.Station;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslationHelper;

/**
 * Responsible for mapping GTFS Stop into the OTP model.
 * <p>
 * NOTE! This class has state. This class also holds a index of all mapped stops to avoid
 * mapping the same stop twice. We do this because the library (onebusaway) return transfers with
 * Stop object references, not stop ids. Instead of looking up the Stops by id in the {@link
 * TransferMapper} we just use the this class to cache stops. This way, the order of which stops
 * and transfers are mapped does not matter.
 */
class StationMapper {

  /** @see StationMapper (this class JavaDoc) for way we need this. */
  private Map<org.onebusaway.gtfs.model.Stop, Station> mappedStops = new HashMap<>();

  /** Map from GTFS to OTP model, {@code null} safe. */
  Station map(org.onebusaway.gtfs.model.Stop original) {
    return map(original, null);
  }

  Station map(org.onebusaway.gtfs.model.Stop original, TranslationHelper translationHelper) {
    return original == null ? null : mappedStops.computeIfAbsent(original, k -> doMap(original, translationHelper));
  }

  private Station doMap(org.onebusaway.gtfs.model.Stop rhs, TranslationHelper translationHelper) {
    if (rhs.getLocationType() != org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STATION) {
      throw new IllegalArgumentException(
          "Expected type " + org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STATION + ", but got "
              + rhs.getLocationType());
    }

    if (translationHelper != null) {
      return new Station(
              mapAgencyAndId(rhs.getId()),
              translationHelper.getTranslation(TranslationHelper.TABLE_STOPS,
                      TranslationHelper.STOP_NAME, rhs.getId().getId(),
                      null, rhs.getName()
              ),
              WgsCoordinateMapper.mapToDomain(rhs),
              rhs.getCode(),
              rhs.getDesc(),
              translationHelper.getTranslation(TranslationHelper.TABLE_STOPS,
                      TranslationHelper.STOP_URL, rhs.getId().getId(),
                      null, rhs.getUrl()
              ),
              rhs.getTimezone() == null ? null : TimeZone.getTimeZone(rhs.getTimezone()),
              // Use default cost priority
              null
      );
    }

    return new Station(
        mapAgencyAndId(rhs.getId()),
        new NonLocalizedString(rhs.getName()),
        WgsCoordinateMapper.mapToDomain(rhs),
        rhs.getCode(),
        rhs.getDesc(),
        new NonLocalizedString(rhs.getUrl()),
        rhs.getTimezone() == null ? null : TimeZone.getTimeZone(rhs.getTimezone()),
        // Use default cost priority
        null
    );
  }
}
