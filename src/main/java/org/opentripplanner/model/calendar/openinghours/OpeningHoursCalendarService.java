package org.opentripplanner.model.calendar.openinghours;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class OpeningHoursCalendarService implements Serializable {

  private final Deduplicator deduplicator;
  private final LocalDate startOfPeriod;
  private final int daysInPeriod;

  public OpeningHoursCalendarService(
    Deduplicator deduplicator,
    LocalDate startOfPeriod,
    LocalDate endOfPeriod
  ) {
    this.deduplicator = deduplicator;
    this.startOfPeriod = startOfPeriod;
    this.daysInPeriod = (int) ChronoUnit.DAYS.between(startOfPeriod, endOfPeriod);
  }

  public OHCalendarBuilder newBuilder(ZoneId zoneId) {
    return new OHCalendarBuilder(deduplicator, startOfPeriod, daysInPeriod, zoneId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(deduplicator, startOfPeriod, daysInPeriod);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final OpeningHoursCalendarService that = (OpeningHoursCalendarService) o;
    return (
      deduplicator.equals(that.deduplicator) &&
      startOfPeriod.equals(that.startOfPeriod) &&
      daysInPeriod == daysInPeriod
    );
  }
}
