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

import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class ServiceCalendarDateMapper {
    private Map<org.onebusaway.gtfs.model.ServiceCalendarDate, ServiceCalendarDate> mappedServiceDates = new HashMap<>();

    Collection<ServiceCalendarDate> map(
            Collection<org.onebusaway.gtfs.model.ServiceCalendarDate> allServiceDates) {
        return MapUtils.mapToList(allServiceDates, this::map);
    }

    ServiceCalendarDate map(org.onebusaway.gtfs.model.ServiceCalendarDate orginal) {
        return orginal == null ? null : mappedServiceDates.computeIfAbsent(orginal, this::doMap);
    }

    private ServiceCalendarDate doMap(org.onebusaway.gtfs.model.ServiceCalendarDate rhs) {
        ServiceCalendarDate lhs = new ServiceCalendarDate();

        lhs.setId(rhs.getId());
        lhs.setServiceId(AgencyAndIdMapper.mapAgencyAndId(rhs.getServiceId()));
        lhs.setDate(ServiceDateMapper.mapServiceDate(rhs.getDate()));
        lhs.setExceptionType(rhs.getExceptionType());

        return lhs;
    }

}
