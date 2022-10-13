package org.opentripplanner.ext.vehicleparking.kml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

public class KmlBikeParkSourceTest {

  private static final String TEST_FEED_ID = "testFeed";

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
    assertTrue(
      alkmaarCoordinate.longitude() >= 4.739850 && alkmaarCoordinate.longitude() <= 4.739851
    );
    assertTrue(
      alkmaarCoordinate.latitude() >= 52.637531 && alkmaarCoordinate.latitude() <= 52.637532
    );
    var zwolleCoordinate = zwolle.getCoordinate();
    assertTrue(
      zwolleCoordinate.longitude() >= 6.091060 && zwolleCoordinate.longitude() <= 6.091061
    );
    assertTrue(
      zwolleCoordinate.latitude() >= 52.504990 && zwolleCoordinate.latitude() <= 52.504991
    );
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
    assertTrue(
      alkmaarCoordinate.longitude() >= 4.739850 && alkmaarCoordinate.longitude() <= 4.739851
    );
    assertTrue(
      alkmaarCoordinate.latitude() >= 52.637531 && alkmaarCoordinate.latitude() <= 52.637532
    );
    var almereCoordinate = almere.getCoordinate();
    assertTrue(almereCoordinate.longitude() >= 5.21780 && almereCoordinate.longitude() <= 5.21782);
    assertTrue(
      almereCoordinate.latitude() >= 52.3746190 && almereCoordinate.latitude() <= 52.3746191
    );
  }
}
