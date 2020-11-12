package org.opentripplanner.netex.mapping.calendar;

import org.opentripplanner.model.calendar.ServiceDate;
import org.rutebanken.netex.model.OperatingDay;

import javax.validation.constraints.NotNull;

class OperatingDayMapper {

  /** Utility class, prevent instantiation. */
  private OperatingDayMapper() { }

  static ServiceDate map(@NotNull OperatingDay opDay) {
    return new ServiceDate(opDay.getCalendarDate().toLocalDate());
  }
}
