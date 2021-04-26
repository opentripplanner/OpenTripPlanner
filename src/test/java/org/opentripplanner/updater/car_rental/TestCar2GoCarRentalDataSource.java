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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.routing.car_rental.CarFuelType;
import org.opentripplanner.routing.car_rental.CarRentalRegion;
import org.opentripplanner.routing.car_rental.CarRentalStation;

import java.io.IOException;
import java.util.List;

public class TestCar2GoCarRentalDataSource extends TestCase {
    private ObjectMapper mapper = new ObjectMapper();
    private ObjectNode config;

    @BeforeClass
    public void setUp() {
        config = mapper.createObjectNode();
        config.put("vehiclesUrl", "file:src/test/resources/car/car2go.json");
        config.put("regionsUrl", "file:src/test/resources/car/region.json");
    }

    @Test
    public void testParseVehiclesJson() {
        Car2GoCarRentalDataSource car2GoCarRentalDataSource = new Car2GoCarRentalDataSource();
        car2GoCarRentalDataSource.configure(null, config);

        // update data source and consume vehicles json
        car2GoCarRentalDataSource.update();
        List<CarRentalStation> rentalStations = car2GoCarRentalDataSource.getStations();
        assertEquals(3, rentalStations.size());
        for (CarRentalStation rentalStation : rentalStations) {
            System.out.println(rentalStation);
        }

        // make sure the first vehicle looks like it should
        CarRentalStation firstVehicle = rentalStations.get(0);
        assertEquals("6623 SE 17th Ave, Portland, OR 97202, USA", firstVehicle.address);
        assertEquals(true, firstVehicle.allowPickup);
        assertEquals(false, firstVehicle.allowDropoff);
        assertEquals(CarFuelType.GASOLINE, firstVehicle.fuelType);
        assertEquals(1, firstVehicle.carsAvailable);
        assertEquals("TEST1", firstVehicle.id);
        assertEquals(true, firstVehicle.isFloatingCar);
        assertEquals("TEST1", firstVehicle.licensePlate);
        assertEquals("TEST1", firstVehicle.name.toString());
        assertEquals(true, firstVehicle.networks.contains("CAR2GO"));
        assertEquals(0, firstVehicle.spacesAvailable);
        assertEquals(-122.64797026, firstVehicle.x);
        assertEquals(45.47517085, firstVehicle.y);
    }

    @Test
    public void testParseRegionJson() throws IOException {
        Car2GoCarRentalDataSource car2GoCarRentalDataSource = new Car2GoCarRentalDataSource();
        car2GoCarRentalDataSource.configure(null, config);

        car2GoCarRentalDataSource.update();
        CarRentalRegion firstRegion = car2GoCarRentalDataSource.getRegions().get(0);

        // verify integrity of region, by checking if a particular point exists inside it
        GeometryFactory geometryFactory = new GeometryFactory();
        assertEquals(
            true,
            firstRegion.geometry.contains(geometryFactory.createPoint(new Coordinate( -122.64759, 45.530162)))
        );
    }
}
