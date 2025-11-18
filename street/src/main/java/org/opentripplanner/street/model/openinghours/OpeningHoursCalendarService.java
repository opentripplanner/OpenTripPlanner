package org.opentripplanner.street.model.openinghours;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.core.framework.di.TransitServicePeriod;
import org.opentripplanner.core.model.time.LocalDateInterval;

public class OpeningHoursCalendarService implements Serializable {

  private final DeduplicatorService deduplicator;
  private final LocalDate startOfPeriod;
  private final int daysInPeriod;

  @Inject
  public OpeningHoursCalendarService(
    DeduplicatorService deduplicator,
    @TransitServicePeriod LocalDateInterval transitServicePeriod
  ) {
    this.deduplicator = deduplicator;
    this.startOfPeriod = transitServicePeriod.getStart();
    this.daysInPeriod = transitServicePeriod.daysInPeriod();
  }

  public OpeningHoursCalendarService(
    DeduplicatorService deduplicator,
    LocalDate startOfPeriod,
    LocalDate endOfPeriod
  ) {
    this(deduplicator, new LocalDateInterval(startOfPeriod, endOfPeriod));
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
      daysInPeriod == that.daysInPeriod
    );
  }
}
