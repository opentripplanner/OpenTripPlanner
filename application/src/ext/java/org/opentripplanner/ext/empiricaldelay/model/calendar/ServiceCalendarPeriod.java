package org.opentripplanner.ext.empiricaldelay.model.calendar;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class only check if a service is inside a period, [start, end] both inclusive. This
 * does not check if a {@code serviceDate} is on a given week day, that is the responsibility
 * of the {@link EmpiricalDelayCalendar}. Together these two classes implement the
 * responsibilities described in the GTFS specification for calendar.txt
 * (https://gtfs.org/documentation/schedule/reference/#calendartxt).
 * <p/>
 * Note! This implementation does not support the GTFS calendar_dates.txt features (adding
 * exceptions for specific days).
 */
public class ServiceCalendarPeriod implements Serializable {

  private final String serviceId;
  private final LocalDate start;
  private final LocalDate end;

  ServiceCalendarPeriod(String serviceId, LocalDate start, LocalDate end) {
    this.serviceId = serviceId;
    this.start = start;
    this.end = end;
  }

  public boolean accept(LocalDate serviceDate) {
    // The Java lib does not support isBeforeOrEquals, hence the ackward !(...)
    return !(start.isAfter(serviceDate) || end.isBefore(serviceDate));
  }

  public String serviceId() {
    return serviceId;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ServiceCalendarPeriod that = (ServiceCalendarPeriod) o;
    return (
      Objects.equals(serviceId, that.serviceId) &&
      Objects.equals(start, that.start) &&
      Objects.equals(end, that.end)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceId, start, end);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(ServiceCalendarPeriod.class)
      .addStr("serviceId", serviceId)
      .addObj("start", start)
      .addObj("end", end)
      .toString();
  }
}
