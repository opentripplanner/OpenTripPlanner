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

package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.ServiceCalendar;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

class ServiceCalendarMapper {
    private Map<org.onebusaway.gtfs.model.ServiceCalendar, ServiceCalendar> mappedCalendars = new HashMap<>();

    Collection<ServiceCalendar> map(
            Collection<org.onebusaway.gtfs.model.ServiceCalendar> allServiceCalendars) {
        return MapUtils.mapToList(allServiceCalendars, this::map);
    }

    ServiceCalendar map(org.onebusaway.gtfs.model.ServiceCalendar orginal) {
        return orginal == null ? null : mappedCalendars.computeIfAbsent(orginal, this::doMap);
    }

    private ServiceCalendar doMap(org.onebusaway.gtfs.model.ServiceCalendar rhs) {
        ServiceCalendar lhs = new ServiceCalendar();

        lhs.setServiceId(mapAgencyAndId(rhs.getServiceId()));
        lhs.setMonday(rhs.getMonday());
        lhs.setTuesday(rhs.getTuesday());
        lhs.setWednesday(rhs.getWednesday());
        lhs.setThursday(rhs.getThursday());
        lhs.setFriday(rhs.getFriday());
        lhs.setSaturday(rhs.getSaturday());
        lhs.setSunday(rhs.getSunday());
        lhs.setStartDate(ServiceDateMapper.mapServiceDate(rhs.getStartDate()));
        lhs.setEndDate(ServiceDateMapper.mapServiceDate(rhs.getEndDate()));

        return lhs;
    }
}
