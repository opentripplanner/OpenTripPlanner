package org.opentripplanner.updater.vehicle_position;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.updater.spi.HttpHeaders;

public class VehiclePositionParsingTest {

  @Test
  public void parseFirstPositionsFeed() {
    var vehiclePositionSource = getVehiclePositionSource("king-county-metro-1.pb");
    var positions = vehiclePositionSource.getPositions();

    Assertions.assertNotNull(positions);

    assertEquals(627, positions.size());

    var first = positions.get(0);
    assertEquals("49195152", first.getTrip().getTripId());
  }

  @Test
  public void parseSecondPositionsFeed() {
    var vehiclePositionSource = getVehiclePositionSource("king-county-metro-2.pb");
    var positions = vehiclePositionSource.getPositions();

    Assertions.assertNotNull(positions);

    assertEquals(570, positions.size());

    var first = positions.get(0);
    assertEquals("49195157", first.getTrip().getTripId());
  }

  private GtfsRealtimeHttpVehiclePositionSource getVehiclePositionSource(String filename) {
    try {
      return new GtfsRealtimeHttpVehiclePositionSource(
        ResourceLoader.url("/gtfs-rt/vehicle-positions/" + filename).toURI(),
        HttpHeaders.empty()
      );
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
