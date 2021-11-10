package org.opentripplanner.updater.vehicle_parking;

import junit.framework.TestCase;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

import java.util.List;

public class TestKmlBikeParkSource extends TestCase {

  private static final String TEST_FEED_ID = "testFeed";

  public void testKML() {

    KmlBikeParkDataSource kmlDataSource = new KmlBikeParkDataSource(new KmlBikeParkDataSource.Parameters() {
      @Override public String getNamePrefix() {
        return null;
      }
      @Override public String getFeedId() {
        return TEST_FEED_ID;
      }
      @Override public boolean zip() {
        return false;
      }
      @Override public String getUrl() {
        return "file:src/test/resources/bike/NSFietsenstallingen.kml";
      }
    });
    assertTrue(kmlDataSource.update());
    List<VehicleParking> bikeParks = kmlDataSource.getVehicleParkings();
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

      KmlBikeParkDataSource kmlDataSource = new KmlBikeParkDataSource(new KmlBikeParkDataSource.Parameters() {
          @Override public String getNamePrefix() {
              return null;
          }
        @Override public String getFeedId() {
          return TEST_FEED_ID;
        }
          @Override public boolean zip() {
              return false;
          }
          @Override public String getUrl() {
              return "file:src/test/resources/bike/NSFietsenstallingen_folder.kml";
          }
      });
    assertTrue(kmlDataSource.update());
    List<VehicleParking> bikeParks = kmlDataSource.getVehicleParkings();
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
