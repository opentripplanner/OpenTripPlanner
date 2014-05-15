/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ServiceCalendar;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlRootElement(name = "Calendar")
public class ServiceCalendarType {

    public ServiceCalendarType(AgencyAndId serviceId, int monday, int tuesday, int wednesday,
            int thursday, int friday, int saturday, int sunday, long startDate, long endDate) {
        this.serviceId = serviceId;
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
        this.saturday = saturday;
        this.sunday = sunday;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public ServiceCalendarType(ServiceCalendar arg) {
        this.serviceId = arg.getServiceId();
        this.monday = arg.getMonday();
        this.tuesday = arg.getTuesday();
        this.wednesday = arg.getWednesday();
        this.thursday = arg.getThursday();
        this.friday = arg.getFriday();
        this.saturday = arg.getSaturday();
        this.sunday = arg.getSunday();
        this.startDate = arg.getStartDate().getAsDate().getTime();
        this.endDate = arg.getEndDate().getAsDate().getTime();
    }

    public ServiceCalendarType() {
    }

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    AgencyAndId serviceId;

    @XmlAttribute
    @JsonSerialize
    Integer monday;

    @XmlAttribute
    @JsonSerialize
    Integer tuesday;

    @XmlAttribute
    @JsonSerialize
    Integer wednesday;

    @XmlAttribute
    @JsonSerialize
    Integer thursday;

    @XmlAttribute
    @JsonSerialize
    Integer friday;

    @XmlAttribute
    @JsonSerialize
    Integer saturday;

    @XmlAttribute
    @JsonSerialize
    Integer sunday;

    @XmlAttribute
    @JsonSerialize
    Long startDate;

    @XmlAttribute
    @JsonSerialize
    Long endDate;

}