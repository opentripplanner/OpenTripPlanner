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

package org.opentripplanner.routing.car_park;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CarParkService implements Serializable {
    private static final long serialVersionUID = -1288992939159246764L;

    private Set<CarPark> carParks = new HashSet<CarPark>();

    public Collection<CarPark> getCarParks() {
        return carParks;
    }

    public void addCarPark(CarPark carPark) {
        // Remove old reference first, as adding will be a no-op if already present
        carParks.remove(carPark);
        carParks.add(carPark);
    }

    public void removeCarPark(CarPark carPark) {
        carParks.remove(carPark);
    }
}
