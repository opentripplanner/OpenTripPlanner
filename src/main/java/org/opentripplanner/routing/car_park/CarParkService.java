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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CarParkService implements Serializable {
    private static final long serialVersionUID = -1288992939159246764L;

    private Set<CarPark> carParks = new HashSet<CarPark>();

    private Map<String, CarPark> carParkById;

    public Collection<CarPark> getCarParks() {
        return carParks;
    }

    public Map<String, CarPark> getCarParkById() {
        return carParkById;
    }

    public void addCarPark(CarPark carPark) {
        // Remove old reference first, as adding will be a no-op if already present
        carParks.remove(carPark);
        carParks.add(carPark);
        reindex();
    }

    public void removeCarPark(CarPark carPark) {
        carParks.remove(carPark);
        reindex();
    }

    private void reindex() {
        carParkById = carParks.stream()
            .collect(Collectors.toMap(carPark -> carPark.id, carPark -> carPark));
    }
}
