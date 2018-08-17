/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import org.opentripplanner.model.calendar.ServiceDate;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author bdferris
 *
 */
public final class ServiceCalendarDate implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int EXCEPTION_TYPE_ADD = 1;

    public static final int EXCEPTION_TYPE_REMOVE = 2;

    private FeedScopedId serviceId;

    private ServiceDate date;

    private int exceptionType;

    public FeedScopedId getServiceId() {
        return serviceId;
    }

    public void setServiceId(FeedScopedId serviceId) {
        this.serviceId = serviceId;
    }

    public ServiceDate getDate() {
        return date;
    }

    public void setDate(ServiceDate date) {
        this.date = date;
    }

    public int getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(int exceptionType) {
        this.exceptionType = exceptionType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
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
}
