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

package org.opentripplanner.routing.edgetype;

import org.onebusaway.gtfs.model.Trip;

/**
 * An edge implementing TimeDependentHeadsign has a headsign that may be dependent on the traversal
 * time, for instance a transit line with stop_headsign set differently at the same stop on the
 * same pattern at different times of day. The impetus for this is that trips with time dependent
 * headsigns cannot return anything meaningful from getDirection, so this interface overloads
 * getDirection and code in {@link org.opentripplanner.routing.core.State#getBackDirection()}.
 * 
 * @author mattwigway
 */
public interface TimeDependentTrip {
    /**
     * Get the direction for the given trip index on the edge
     */
    public String getDirection (int tripIndex);
    
    /**
     * Get the Trip related to the given index
     */
    public Trip getTrip (int tripIndex);
}
