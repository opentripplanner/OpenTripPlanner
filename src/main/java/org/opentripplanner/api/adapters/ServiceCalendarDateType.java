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
import org.onebusaway.gtfs.model.ServiceCalendarDate;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlRootElement(name = "CalendarDate")
public class ServiceCalendarDateType {

    public ServiceCalendarDateType(AgencyAndId serviceId, long date, int exceptionType) {
        this.serviceId = serviceId;
        this.date = date;
        this.exceptionType = exceptionType;
        switch (this.exceptionType) {
        case 1:
            this.exception = "Remove";
            break;
        case 2:
            this.exception = "Add";
            break;
        default:
            this.exception = "";
        }
    }

    public ServiceCalendarDateType(ServiceCalendarDate arg) {
        this.serviceId = arg.getServiceId();
        this.date = arg.getDate().getAsDate().getTime();
        this.exceptionType = arg.getExceptionType();
        switch (this.exceptionType) {
        case 1:
            this.exception = "Remove";
            break;
        case 2:
            this.exception = "Add";
            break;
        default:
            this.exception = "";
        }
    }

    public ServiceCalendarDateType() {
    }

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    AgencyAndId serviceId;

    @XmlAttribute
    @JsonSerialize
    Long date;

    @XmlAttribute
    @JsonSerialize
    Integer exceptionType;

    @XmlAttribute
    @JsonSerialize
    String exception;

}