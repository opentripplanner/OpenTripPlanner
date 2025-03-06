/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.calendar;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class explicitly activate or disable a service by date. It can be used in two ways.
 * <ol>
 *     <li>in conjunction with {@link ServiceCalendar} to define exceptions to the default service
 *     patterns defined.
 *     <li>Omit {@link ServiceCalendar} and use this class to specify each date of service.
 * </ol>
 * <p>
 * This class is immutable.
 */
public final class ServiceCalendarDate implements Serializable, Comparable<ServiceCalendarDate> {

  /**
   * Service has been added for the specified date.
   */
  public static final int EXCEPTION_TYPE_ADD = 1;

  /**
   * Service has been removed for the specified date.
   */
  public static final int EXCEPTION_TYPE_REMOVE = 2;

  private final FeedScopedId serviceId;

  private final LocalDate date;

  private final int exceptionType;

  public ServiceCalendarDate(FeedScopedId serviceId, LocalDate date, int exceptionType) {
    this.serviceId = serviceId;
    this.date = date;
    this.exceptionType = exceptionType;
  }

  /**
   * Create a service calendar date on the given date with the given id. The 'exceptionType' is set
   * to 'EXCEPTION_TYPE_ADD'.
   */
  public static ServiceCalendarDate create(FeedScopedId serviceId, LocalDate date) {
    return new ServiceCalendarDate(serviceId, date, EXCEPTION_TYPE_ADD);
  }

  public FeedScopedId getServiceId() {
    return serviceId;
  }

  public LocalDate getDate() {
    return date;
  }

  public int getExceptionType() {
    return exceptionType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceId, date);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceCalendarDate that = (ServiceCalendarDate) o;
    return Objects.equals(serviceId, that.serviceId) && Objects.equals(date, that.date);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(ServiceCalendarDate.class)
      .addObj("serviceId", this.serviceId)
      .addObj("date", this.date)
      .addObj("exception", this.exceptionType)
      .toString();
  }

  /**
   * Default sort order is to sort on {@code serviceId} first and then on {@code date}.
   */
  @Override
  public int compareTo(ServiceCalendarDate other) {
    int c = serviceId.compareTo(other.serviceId);
    if (c == 0) {
      c = date.compareTo(other.date);
    }
    return c;
  }
}
