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

package org.opentripplanner.routing.trippattern;

import lombok.Getter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

public class AbstractUpdate {

    /** The trip this update applies to */
    @Getter
    protected final AgencyAndId tripId;

    /** The official timestamp for the update, if one was provided, or the time it was received. */
    @Getter
    protected final long timestamp;

    /** The service date this update applies to. */
    @Getter
    protected final ServiceDate serviceDate;
    
    public AbstractUpdate(AgencyAndId tripId, long timestamp, ServiceDate serviceDate) {
        if(tripId == null)
            throw new IllegalArgumentException("A tripId must be specified");
        if(serviceDate == null)
            throw new IllegalArgumentException("A serviceDate must be specified");
        
        this.tripId = tripId;
        this.timestamp = timestamp;
        this.serviceDate = serviceDate;
    }
}
