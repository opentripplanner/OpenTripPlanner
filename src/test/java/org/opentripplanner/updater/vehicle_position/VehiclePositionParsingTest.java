package org.opentripplanner.updater.vehicle_position;

import java.net.URI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VehiclePositionParsingTest {

  @Test
  public void parseFirstPositionsFeed() {
    var vehiclePositionSource = getVehiclePositionSource("king-county-metro-1.pb");
    var positions = vehiclePositionSource.getPositions();

    Assertions.assertNotNull(positions);

    Assertions.assertEquals(627, positions.size());

    var first = positions.get(0);
    Assertions.assertEquals("49195152", first.getTrip().getTripId());
  }

  @Test
  public void parseSecondPositionsFeed() {
    var vehiclePositionSource = getVehiclePositionSource("king-county-metro-2.pb");
    var positions = vehiclePositionSource.getPositions();

    Assertions.assertNotNull(positions);

    Assertions.assertEquals(570, positions.size());

    var first = positions.get(0);
    Assertions.assertEquals("49195157", first.getTrip().getTripId());
  }

  private GtfsRealtimeHttpVehiclePositionSource getVehiclePositionSource(String filename) {
    return new GtfsRealtimeHttpVehiclePositionSource(
      URI.create("file:src/test/resources/vehicle-positions/" + filename)
    );
  }
}
