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

package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.HashMap;

import org.onebusaway.gtfs.model.AgencyAndId;

public class ServiceIdToNumberService implements Serializable {
    private static final long serialVersionUID = -8447673406675368532L;
    
    HashMap<AgencyAndId, Integer> numberForServiceId;
    
    
    public ServiceIdToNumberService(HashMap<AgencyAndId, Integer> serviceIds) {
        this.numberForServiceId = serviceIds;
    }


    public int getNumber(AgencyAndId serviceId) {
        Integer number = numberForServiceId.get(serviceId);
        if (number == null) {
            return -1;
        }
        return number;
    }

}
