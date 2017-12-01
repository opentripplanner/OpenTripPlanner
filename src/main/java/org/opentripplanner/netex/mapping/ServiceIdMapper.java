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
package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;

import javax.xml.bind.JAXBElement;

public class ServiceIdMapper {

    public static String mapToServiceId(DayTypeRefs_RelStructure dayTypes) {
        StringBuilder serviceId = new StringBuilder();
        boolean first = true;
        for (JAXBElement dt : dayTypes.getDayTypeRef()) {
            if (!first) {
                serviceId.append("+");
            }
            first = false;
            if (dt.getValue() instanceof DayTypeRefStructure) {
                DayTypeRefStructure dayType = (DayTypeRefStructure) dt.getValue();
                serviceId.append(dayType.getRef());
            }
        }
        return serviceId.toString();
    }
}
