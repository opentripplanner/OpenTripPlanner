package org.opentripplanner.gtfs.mapping;

import java.time.LocalDate;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.core.model.time.LocalDateRange;

/** Responsible for mapping GTFS ServiceDate into the OTP model. */
class ServiceDateMapper {

  /** Map from GTFS ServiceDate to a {@link java.time.LocalDate}, {@code null} safe. */
  static LocalDate mapLocalDate(ServiceDate orginal) {
    return orginal == null
      ? null
      : LocalDate.of(orginal.getYear(), orginal.getMonth(), orginal.getDay());
  }

  static LocalDateRange mapServiceDateInterval(ServiceDate start, ServiceDate end) {
    return LocalDateRange.ofInclusiveEnd(mapLocalDate(start), mapLocalDate(end));
  }
}
