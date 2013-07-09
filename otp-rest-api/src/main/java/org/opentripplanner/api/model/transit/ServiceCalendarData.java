/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.model.transit;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;

public class ServiceCalendarData {
    @XmlElement
    public List<ServiceCalendar> calendars;

    @XmlElement
    public List<ServiceCalendarDate> calendarDates;
}
