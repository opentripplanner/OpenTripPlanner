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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.routing.car_rental.CarFuelType;
import org.opentripplanner.routing.car_rental.CarRentalStation;

import java.util.List;

public class TestReachNowRentalDataSource extends TestCase {
    private ObjectMapper mapper = new ObjectMapper();
    private ObjectNode config;

    @BeforeClass
    public void setUp() {
        config = mapper.createObjectNode();
        config.put("vehiclesUrl", "file:src/test/resources/car/reachNowVehicles.json");
        config.put("regionsUrl", "file:src/test/resources/car/region.json");
    }

    @Test
    public void testParseVehiclesJson() throws Exception {
        ReachNowCarRentalDataSource reachNowCarRentalDataSource = new ReachNowCarRentalDataSource();
        reachNowCarRentalDataSource.configure(null, config);

        // update data source and consume vehicles json
        assertTrue(reachNowCarRentalDataSource.updateStations());
        List<CarRentalStation> rentalStations = reachNowCarRentalDataSource.getStations();
        assertEquals(2, rentalStations.size());
        for (CarRentalStation rentalStation : rentalStations) {
            System.out.println(rentalStation);
        }

        // make sure the first vehicle looks like it should
        CarRentalStation firstVehicle = rentalStations.get(0);
        assertEquals("3123 SE 62nd Ave", firstVehicle.address);
        assertEquals(true, firstVehicle.allowPickup);
        assertEquals(false, firstVehicle.allowDropoff);
        assertEquals(CarFuelType.ELECTRIC, firstVehicle.fuelType);
        assertEquals(1, firstVehicle.carsAvailable);
        assertEquals("958", firstVehicle.id);
        assertEquals(true, firstVehicle.isFloatingCar);
        assertEquals("TEST1", firstVehicle.licensePlate);
        assertEquals("TEST1", firstVehicle.name.toString());
        assertEquals(true, firstVehicle.networks.contains("REACHNOW"));
        assertEquals(0, firstVehicle.spacesAvailable);
        assertEquals(-122.599453, firstVehicle.x);
        assertEquals(45.499904, firstVehicle.y);
    }
}
