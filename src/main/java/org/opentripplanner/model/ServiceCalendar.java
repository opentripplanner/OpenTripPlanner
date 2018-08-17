/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import org.opentripplanner.model.calendar.ServiceDate;

import java.io.Serializable;
import java.util.Objects;

/**
 * Note that I decided to call this class ServiceCalendar instead of Calendar,
 * so as to avoid confusion with java.util.Calendar
 *
 * @author bdferris
 *
 */
public final class ServiceCalendar implements Serializable {

    private static final long serialVersionUID = 1L;

    private FeedScopedId serviceId;

    private int monday;

    private int tuesday;

    private int wednesday;

    private int thursday;

    private int friday;

    private int saturday;

    private int sunday;

    private ServiceDate startDate;

    private ServiceDate endDate;

    public FeedScopedId getServiceId() {
        return serviceId;
    }

    public void setServiceId(FeedScopedId serviceId) {
        this.serviceId = serviceId;
    }

    public int getMonday() {
        return monday;
    }

    public void setMonday(int monday) {
        this.monday = monday;
    }

    public int getTuesday() {
        return tuesday;
    }

    public void setTuesday(int tuesday) {
        this.tuesday = tuesday;
    }

    public int getWednesday() {
        return wednesday;
    }

    public void setWednesday(int wednesday) {
        this.wednesday = wednesday;
    }

    public int getThursday() {
        return thursday;
    }

    public void setThursday(int thursday) {
        this.thursday = thursday;
    }

    public int getFriday() {
        return friday;
    }

    public void setFriday(int friday) {
        this.friday = friday;
    }

    public int getSaturday() {
        return saturday;
    }

    public void setSaturday(int saturday) {
        this.saturday = saturday;
    }

    public int getSunday() {
        return sunday;
    }

    public void setSunday(int sunday) {
        this.sunday = sunday;
    }

    public ServiceDate getStartDate() {
        return startDate;
    }

    public void setStartDate(ServiceDate startDate) {
        this.startDate = startDate;
    }

    public ServiceDate getEndDate() {
        return endDate;
    }

    public void setEndDate(ServiceDate endDate) {
        this.endDate = endDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ServiceCalendar that = (ServiceCalendar) o;
        return monday == that.monday && tuesday == that.tuesday && wednesday == that.wednesday
                && thursday == that.thursday && friday == that.friday && saturday == that.saturday
                && sunday == that.sunday && Objects.equals(serviceId, that.serviceId) && Objects
                .equals(startDate, that.startDate) && Objects.equals(endDate, that.endDate);
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(serviceId, monday, tuesday, wednesday, thursday, friday, saturday, sunday,
                        startDate, endDate);
    }

    public String toString() {
        return "<ServiceCalendar " + this.serviceId + " [" + this.monday + this.tuesday
                + this.wednesday + this.thursday + this.friday + this.saturday + this.sunday + "]>";
    }
}
