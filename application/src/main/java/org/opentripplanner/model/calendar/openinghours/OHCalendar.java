package org.opentripplanner.model.calendar.openinghours;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class OHCalendar implements Serializable {

  private final ZoneId zoneId;
  private final List<OpeningHours> openingHours;
  private final LocalDate startOfCalendar;
  private final LocalDate endOfCalendar;

  public OHCalendar(
    LocalDate startOfCalendar,
    LocalDate endOfCalendar,
    ZoneId zoneId,
    List<OpeningHours> openingHours
  ) {
    this.startOfCalendar = startOfCalendar;
    this.endOfCalendar = endOfCalendar;
    this.zoneId = zoneId;
    this.openingHours = openingHours;
  }

  public boolean isOpen(long timeEpochSecond) {
    ZonedDateTime searchDateTime = Instant.ofEpochSecond(timeEpochSecond).atZone(zoneId);
    LocalDate searchDate = searchDateTime.toLocalDate();
    int daysFromStart = (int) ChronoUnit.DAYS.between(startOfCalendar, searchDate);
    int daysUntilEnd = (int) ChronoUnit.DAYS.between(searchDate, endOfCalendar);
    if (daysFromStart < 0 || daysUntilEnd < 0) {
      return false;
    }
    int secondsFromMidnight = searchDateTime.toLocalTime().toSecondOfDay();
    return openingHours
      .stream()
      .anyMatch(openingHoursDefinition ->
        openingHoursDefinition.isOpen(daysFromStart, secondsFromMidnight)
      );
  }

  @Override
  public int hashCode() {
    return Objects.hash(zoneId, openingHours, startOfCalendar, endOfCalendar);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final OHCalendar that = (OHCalendar) o;
    return (
      openingHours.equals(that.openingHours) &&
      zoneId.equals(that.zoneId) &&
      startOfCalendar.equals(that.startOfCalendar) &&
      endOfCalendar.equals(that.endOfCalendar)
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(OHCalendar.class)
      .addObj("zoneId", zoneId)
      .addCol("openingHours", openingHours)
      .toString();
  }

  public List<OpeningHours> openingHours() {
    return openingHours;
  }
}
