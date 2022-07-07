package org.opentripplanner.gtfs.mapping;

import java.time.LocalDate;
import org.opentripplanner.model.calendar.ServiceDateInterval;

/** Responsible for mapping GTFS ServiceDate into the OTP model. */
class ServiceDateMapper {

  /** Map from GTFS ServiceDate to a {@link java.time.LocalDate}, {@code null} safe. */
  static LocalDate mapLocalDate(org.onebusaway.gtfs.model.calendar.ServiceDate orginal) {
    return orginal == null
      ? null
      : LocalDate.of(orginal.getYear(), orginal.getMonth(), orginal.getDay());
  }

  static ServiceDateInterval mapServiceDateInterval(
    org.onebusaway.gtfs.model.calendar.ServiceDate start,
    org.onebusaway.gtfs.model.calendar.ServiceDate end
  ) {
    return new ServiceDateInterval(mapLocalDate(start), mapLocalDate(end));
  }
}
