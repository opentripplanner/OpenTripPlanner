package org.opentripplanner.netex.mapping.calendar;

import java.time.LocalDate;
import org.entur.netex2.model.OperatingDay;

class OperatingDayMapper {

  /** Utility class, prevent instantiation. */
  private OperatingDayMapper() {}

  static LocalDate map(OperatingDay opDay) {
    return opDay.getCalendarDate();
  }
}
