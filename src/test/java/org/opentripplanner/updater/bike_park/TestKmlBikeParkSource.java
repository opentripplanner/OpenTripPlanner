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
