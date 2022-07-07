package org.opentripplanner.netex.mapping.calendar;

import java.time.LocalDate;
import javax.annotation.Nonnull;
import org.rutebanken.netex.model.OperatingDay;

class OperatingDayMapper {

  /** Utility class, prevent instantiation. */
  private OperatingDayMapper() {}

  static LocalDate map(@Nonnull OperatingDay opDay) {
    return opDay.getCalendarDate().toLocalDate();
  }
}
