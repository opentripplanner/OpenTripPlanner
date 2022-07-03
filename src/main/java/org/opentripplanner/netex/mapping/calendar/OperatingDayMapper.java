package org.opentripplanner.netex.mapping.calendar;

import java.time.LocalDate;
import javax.validation.constraints.NotNull;
import org.rutebanken.netex.model.OperatingDay;

class OperatingDayMapper {

  /** Utility class, prevent instantiation. */
  private OperatingDayMapper() {}

  static LocalDate map(@NotNull OperatingDay opDay) {
    return opDay.getCalendarDate().toLocalDate();
  }
}
