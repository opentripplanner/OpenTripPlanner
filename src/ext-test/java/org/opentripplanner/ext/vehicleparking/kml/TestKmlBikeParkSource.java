package org.opentripplanner.ext.vehicleparking.kml;

import junit.framework.TestCase;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

import java.util.List;

public class TestKmlBikeParkSource extends TestCase {

  private static final String TEST_FEED_ID = "testFeed";

  public void testKML() {
    var parameters = new KmlUpdaterParameters("",
            "file:src/ext-test/resources/vehicleparking/kml/NSFietsenstallingen.kml", TEST_FEED_ID,
            null, 60, false, null
    );
    KmlBikeParkDataSource kmlDataSource = new KmlBikeParkDataSource(parameters);
    assertTrue(kmlDataSource.update());
    List<VehicleParking> bikeParks = kmlDataSource.getUpdates();
    assertEquals(5, bikeParks.size());
    VehicleParking alkmaar = bikeParks.get(0);
    VehicleParking zwolle = bikeParks.get(4);
    assertEquals("Station Alkmaar", alkmaar.getName().toString());
    assertEquals("Station Zwolle", zwolle.getName().toString());
    assertTrue(alkmaar.getX() >= 4.739850 && alkmaar.getX() <= 4.739851);
    assertTrue(alkmaar.getY() >= 52.637531 && alkmaar.getY() <= 52.637532);
    assertTrue(zwolle.getX() >= 6.091060 && zwolle.getX() <= 6.091061);
    assertTrue(zwolle.getY() >= 52.504990 && zwolle.getY() <= 52.504991);
  }

  public void testKMLWithFolder() {
    var parameters = new KmlUpdaterParameters("",
            "file:src/ext-test/resources/vehicleparking/kml/NSFietsenstallingen_folder.kml",
            TEST_FEED_ID,
            null, 60, false, null
    );
    KmlBikeParkDataSource kmlDataSource = new KmlBikeParkDataSource(parameters);
    assertTrue(kmlDataSource.update());
    List<VehicleParking> bikeParks = kmlDataSource.getUpdates();
    assertEquals(5, bikeParks.size());
    VehicleParking alkmaar = bikeParks.get(0);
    VehicleParking almere = bikeParks.get(4);
    assertEquals("Station Alkmaar", alkmaar.getName().toString());
    assertEquals("Station Almere Centrum", almere.getName().toString());
    assertTrue(alkmaar.getX() >= 4.739850 && alkmaar.getX() <= 4.739851);
    assertTrue(alkmaar.getY() >= 52.637531 && alkmaar.getY() <= 52.637532);
    assertTrue(almere.getX() >= 5.21780 && almere.getX() <= 5.21782);
    assertTrue(almere.getY() >= 52.3746190 && almere.getY() <= 52.3746191);
  }

}
