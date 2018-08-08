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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CarParkService implements Serializable {
    private static final long serialVersionUID = -1288992939159246764L;

    private Map<String, CarPark> carParks = new HashMap<>();

    public Collection<CarPark> getCarParks() {
        return carParks.values();
    }

    public Map<String, CarPark> getCarParkById() {
        return carParks;
    }

    public void addCarPark(CarPark carPark) {
        carParks.put(carPark.id, carPark);
    }

    public void removeCarPark(CarPark carPark) {
        carParks.remove(carPark.id);
    }
}
