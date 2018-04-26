/*
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    private FeedId serviceId;

    private ServiceDate date;

    private int exceptionType;

    public FeedId getServiceId() {
        return serviceId;
    }

    public void setServiceId(FeedId serviceId) {
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
