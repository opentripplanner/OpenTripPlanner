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

package org.opentripplanner.updater.car_rental;

import org.opentripplanner.routing.car_rental.CarRentalRegion;
import org.opentripplanner.routing.car_rental.CarRentalStation;

import java.util.List;

public interface CarRentalDataSource {

    /** Update the regions from the source;
     * returns true if there might have been changes */
    boolean updateRegions();

    /** Update the stations from the source;
     * returns true if there might have been changes */
    boolean updateStations();

    /**
     * @return a List of all currently known car rental stations. The updater will use this to update the Graph.
     */
    List<CarRentalStation> getStations();

    /**
     * @return a List of all currently known car rental regions. The updater will use this to update the Graph.
     */
    List<CarRentalRegion> getRegions();

}
