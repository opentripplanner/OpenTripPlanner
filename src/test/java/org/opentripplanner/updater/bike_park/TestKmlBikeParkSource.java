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

package org.opentripplanner.updater.bike_park;

import java.util.List;

import junit.framework.TestCase;

import org.opentripplanner.routing.bike_park.BikePark;

public class TestKmlBikeParkSource extends TestCase {

    public void testKML() {

        KmlBikeParkDataSource kmlDataSource = new KmlBikeParkDataSource();
        kmlDataSource.setUrl("file:src/test/resources/bike/NSFietsenstallingen.kml");
        assertTrue(kmlDataSource.update());
        List<BikePark> bikeParks = kmlDataSource.getBikeParks();
        assertEquals(5, bikeParks.size());
        BikePark alkmaar = bikeParks.get(0);
        BikePark zwolle = bikeParks.get(4);
        assertEquals("Station Alkmaar", alkmaar.name);
        assertEquals("Station Zwolle", zwolle.name);
        assertTrue(alkmaar.x >= 4.739850 && alkmaar.x <= 4.739851);
        assertTrue(alkmaar.y >= 52.637531 && alkmaar.y <= 52.637532);
        assertTrue(zwolle.x >= 6.091060 && zwolle.x <= 6.091061);
        assertTrue(zwolle.y >= 52.504990 && zwolle.y <= 52.504991);
    }

    public void testKMLWithFolder() {

        KmlBikeParkDataSource kmlDataSource = new KmlBikeParkDataSource();
        kmlDataSource.setUrl("file:src/test/resources/bike/NSFietsenstallingen_folder.kml");
        assertTrue(kmlDataSource.update());
        List<BikePark> bikeParks = kmlDataSource.getBikeParks();
        assertEquals(5, bikeParks.size());
        BikePark alkmaar = bikeParks.get(0);
        BikePark almere = bikeParks.get(4);
        assertEquals("Station Alkmaar", alkmaar.name);
        assertEquals("Station Almere Centrum", almere.name);
        assertTrue(alkmaar.x >= 4.739850 && alkmaar.x <= 4.739851);
        assertTrue(alkmaar.y >= 52.637531 && alkmaar.y <= 52.637532);
        assertTrue(almere.x >= 5.21780 && almere.x <= 5.21782);
        assertTrue(almere.y >= 52.3746190 && almere.y <= 52.3746191);
    }

}
