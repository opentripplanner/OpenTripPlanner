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

package org.opentripplanner.updater.bike_rental;

import java.util.List;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;

/**
 * TODO clarify thread safety.
 * It appears that update() and getStations() are never called simultaneously by different threads, but is not stated.
 */
public interface BikeRentalDataSource {

    /**
     * Fetch current data about bike rental stations and availability from this source.
     * @return true if this operation may have changed something in the list of stations.
     */
    boolean update();

    /**
     * @return a List of all currently known bike rental stations. The updater will use this to update the Graph.
     */
    List<BikeRentalStation> getStations();
    
}
