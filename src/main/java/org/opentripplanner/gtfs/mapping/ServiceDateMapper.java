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

import org.opentripplanner.model.calendar.ServiceDate;

class ServiceDateMapper {
    static ServiceDate mapServiceDate(org.onebusaway.gtfs.model.calendar.ServiceDate orginal) {
        return orginal == null ?
                null :
                new ServiceDate(orginal.getYear(), orginal.getMonth(), orginal.getDay());
    }
}
