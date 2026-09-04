package org.opentripplanner.ext.taxizone;

import java.time.LocalDate;
import java.util.Optional;
import org.opentripplanner.ext.taxizone.model.TaxiZone;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Service for looking up which taxi zone provider covers a given pickup-dropoff coordinate pair.
 */
public interface TaxiZoneService {
  /**
   * Returns the first zone whose geometry contains both {@code pickup} and {@code dropoff}, and
   * whose GTFS calendar has {@code date} as a valid service date. Returns an empty optional if no
   * zone covers both endpoints on that date.
   */
  Optional<TaxiZone> findZone(WgsCoordinate pickup, WgsCoordinate dropoff, LocalDate date);
}
