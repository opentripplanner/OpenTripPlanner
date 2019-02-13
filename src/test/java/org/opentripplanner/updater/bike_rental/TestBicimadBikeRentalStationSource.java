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

import junit.framework.TestCase;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

import java.util.List;

public class TestBicimadBikeRentalStationSource extends TestCase {

        public void testBicimad() {

                BicimadBikeRentalDataSource bicimadBikeRentalDataSource = new BicimadBikeRentalDataSource();
                bicimadBikeRentalDataSource.setUrl("file:src/test/resources/bike/bicimad.json");
                assertTrue(bicimadBikeRentalDataSource.update());
                List<BikeRentalStation> rentalStations = bicimadBikeRentalDataSource.getStations();
                assertEquals(rentalStations.size(), 172);
                for (BikeRentalStation rentalStation : rentalStations) {
                        System.out.println(rentalStation);
                }
                BikeRentalStation puertaDelSolA = rentalStations.get(0);
                assertEquals("Puerta del Sol A", puertaDelSolA.name.toString());
                assertEquals("1", puertaDelSolA.id);
                assertEquals(-3.7024255, puertaDelSolA.x);
                assertEquals(40.4168961, puertaDelSolA.y);
                assertEquals(18, puertaDelSolA.spacesAvailable);
                assertEquals(4, puertaDelSolA.bikesAvailable);

                BikeRentalStation plazaDeLavapies = rentalStations.get(55);
                assertEquals("Plaza de Lavapiés", plazaDeLavapies.name.toString());
                assertEquals("57", plazaDeLavapies.id);
                assertEquals(-3.7008803, plazaDeLavapies.x);
                assertEquals(40.4089282, plazaDeLavapies.y);
                assertEquals(1, plazaDeLavapies.spacesAvailable);
                assertEquals(22, plazaDeLavapies.bikesAvailable);
        }
}
