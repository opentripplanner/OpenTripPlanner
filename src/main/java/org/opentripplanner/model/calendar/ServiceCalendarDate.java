/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.calendar;

import org.opentripplanner.model.FeedScopedId;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class explicitly activate or disable a service by date. It can be used in two ways.
 * <ol>
 *     <li>in conjunction with {@link ServiceCalendar} to define exceptions to the default service
 *     patterns defined.
 *     <li>Omit {@link ServiceCalendar} and use this class to specify each date of service.
 * </ol>
 *
 * This class is immutable.
 */
public final class ServiceCalendarDate implements Serializable, Comparable<ServiceCalendarDate> {

    private static final long serialVersionUID = 1L;

    /**
     * Service has been added for the specified date.
     */
    public static final int EXCEPTION_TYPE_ADD = 1;

    /**
     * Service has been removed for the specified date.
     */
    public static final int EXCEPTION_TYPE_REMOVE = 2;

    private final FeedScopedId serviceId;

    private final ServiceDate date;

    private final int exceptionType;

    public ServiceCalendarDate(FeedScopedId serviceId, ServiceDate date, int exceptionType) {
        this.serviceId = serviceId;
        this.date = date;
        this.exceptionType =  exceptionType;
    }

    /**
     * Create a service calendar date on the given date with the given id. The 'exceptionType'
     * is set to 'EXCEPTION_TYPE_ADD'.
     */
    public static ServiceCalendarDate create(FeedScopedId serviceId, ServiceDate date) {
        return new ServiceCalendarDate(serviceId, date, EXCEPTION_TYPE_ADD);
    }

    public FeedScopedId getServiceId() {
        return serviceId;
    }

    public ServiceDate getDate() {
        return date;
    }

    public int getExceptionType() {
        return exceptionType;
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
    public int hashCode() {
        return Objects.hash(serviceId, date);
    }

    @Override
    public String toString() {
        return "<CalendarDate serviceId=" + this.serviceId + " date=" + this.date + " exception="
                + this.exceptionType + ">";
    }

    /**
     * Default sort order is to sort on {@code serviceId} first and then on {@code date}.
     */
    @Override
    public int compareTo(ServiceCalendarDate other) {
        int c = serviceId.compareTo(other.serviceId);
        if(c == 0) {
            c = date.compareTo(other.date);
        }
        return c;
    }
}
