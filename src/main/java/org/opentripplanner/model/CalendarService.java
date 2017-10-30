/*
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.opentripplanner.model;

import org.opentripplanner.model.calendar.ServiceDate;

import java.util.Set;
import java.util.TimeZone;

public interface CalendarService {

    /**
     * @return the set of all service ids used in the data set
     */
    Set<AgencyAndId> getServiceIds();

    /**
     * @param serviceId the target service id
     * @return the set of all service dates for which the specified service id is
     *         active
     */
    Set<ServiceDate> getServiceDatesForServiceId(AgencyAndId serviceId);

    /**
     * Determine the set of service ids that are active on the specified service
     * date.
     *
     * @param date the target service date
     * @return the set of service ids that are active on the specified service
     * date
     */
    Set<AgencyAndId> getServiceIdsOnDate(ServiceDate date);

    /**
     * Returns the instantiated {@link TimeZone} for the specified agency id
     *
     * @param agencyId {@link Agency#getId()}
     * @return the time zone for the specified agency, or null if the agency was
     * not found
     */
    TimeZone getTimeZoneForAgencyId(String agencyId);
}
