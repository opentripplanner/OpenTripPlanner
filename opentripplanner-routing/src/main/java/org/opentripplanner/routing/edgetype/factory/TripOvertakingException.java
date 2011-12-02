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

package org.opentripplanner.routing.edgetype.factory;

import org.onebusaway.gtfs.model.Trip;

public class TripOvertakingException extends RuntimeException {

    /**
     * Thrown when a trip overtakes another trip on the same pattern. 
     */
    private static final long serialVersionUID = 1L;
    public Trip overtaker, overtaken;
    public int stopIndex;
    
    public TripOvertakingException(Trip overtaker, Trip overtaken, int stopIndex) {
        this.overtaker = overtaker;
        this.overtaken = overtaken;
        this.stopIndex = stopIndex;
    }

    @Override
    public String getMessage() {
        return "Possible GTFS feed error: Trip " + overtaker + " overtakes trip " + overtaken
        + " (which has the same stops) at stop index "
        + stopIndex + " This will be handled correctly but inefficiently.";
    }
    
}
