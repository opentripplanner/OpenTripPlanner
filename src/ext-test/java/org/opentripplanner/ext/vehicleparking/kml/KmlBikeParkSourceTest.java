package org.opentripplanner.ext.vehicleparking.kml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

public class KmlBikeParkSourceTest {

  private static final String TEST_FEED_ID = "testFeed";

  private static final double EPSILON = 0.0001;

  @Test
  public void testKML() {
    var parameters = new KmlUpdaterParameters(
      "",
      "file:src/ext-test/resources/vehicleparking/kml/NSFietsenstallingen.kml",
      TEST_FEED_ID,
      null,
      60,
      false,
      null
    );
    KmlBikeParkDataSource kmlDataSource = new KmlBikeParkDataSource(parameters);
    assertTrue(kmlDataSource.update());
    List<VehicleParking> bikeParks = kmlDataSource.getUpdates();
    assertEquals(5, bikeParks.size());
    VehicleParking alkmaar = bikeParks.get(0);
    VehicleParking zwolle = bikeParks.get(4);
    assertEquals("Station Alkmaar", alkmaar.getName().toString());
    assertEquals("Station Zwolle", zwolle.getName().toString());
    var alkmaarCoordinate = alkmaar.getCoordinate();
    assertEquals(4.739850, alkmaarCoordinate.longitude(), EPSILON);
    assertEquals(52.637531, alkmaarCoordinate.latitude(), EPSILON);
    var zwolleCoordinate = zwolle.getCoordinate();
    assertEquals(6.091060, zwolleCoordinate.longitude(), EPSILON);
    assertEquals(52.504990, zwolleCoordinate.latitude(), EPSILON);
  }

  @Test
  public void testKMLWithFolder() {
    var parameters = new KmlUpdaterParameters(
      "",
      "file:src/ext-test/resources/vehicleparking/kml/NSFietsenstallingen_folder.kml",
      TEST_FEED_ID,
      null,
      60,
      false,
      null
    );
    KmlBikeParkDataSource kmlDataSource = new KmlBikeParkDataSource(parameters);
    assertTrue(kmlDataSource.update());
    List<VehicleParking> bikeParks = kmlDataSource.getUpdates();
    assertEquals(5, bikeParks.size());
    VehicleParking alkmaar = bikeParks.get(0);
    VehicleParking almere = bikeParks.get(4);
    assertEquals("Station Alkmaar", alkmaar.getName().toString());
    assertEquals("Station Almere Centrum", almere.getName().toString());
    var alkmaarCoordinate = alkmaar.getCoordinate();
    assertEquals(4.739850, alkmaarCoordinate.longitude(), EPSILON);
    assertEquals(52.637531, alkmaarCoordinate.latitude(), EPSILON);
    var almereCoordinate = almere.getCoordinate();
    assertEquals(5.21780, almereCoordinate.longitude(), EPSILON);
    assertEquals(52.3746190, almereCoordinate.latitude(), EPSILON);
  }
}
