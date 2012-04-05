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

package org.opentripplanner.calendar.impl;

import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.gtfs.services.GtfsRelationalDao;

/**
 * This is actually kind of a hack, and assumes that there is only one copy of CalendarServiceData
 * in the universe.
 * 
 * @author novalis
 * 
 */
public class MultiCalendarServiceImpl extends CalendarServiceImpl {

    public MultiCalendarServiceImpl() {
        setData(new CalendarServiceData());
    }

    public void addData(CalendarServiceData data, GtfsRelationalDao dao) {
        CalendarServiceData _data = super.getData();
        for (Agency agency : dao.getAllAgencies()) {
            String agencyId = agency.getId();
            _data.putTimeZoneForAgencyId(agencyId, data.getTimeZoneForAgencyId(agencyId));
        }
        for (LocalizedServiceId id : data.getLocalizedServiceIds()) {
            _data.putDatesForLocalizedServiceId(id, data.getDatesForLocalizedServiceId(id));
        }
        for (AgencyAndId serviceId : data.getServiceIds()) {
            _data.putServiceDatesForServiceId(serviceId,
                    data.getServiceDatesForServiceId(serviceId));
        }
    }

    public CalendarServiceData getData() {
        return super.getData();
    }

}
