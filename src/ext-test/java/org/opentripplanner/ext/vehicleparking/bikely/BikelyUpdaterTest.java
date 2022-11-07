package org.opentripplanner.ext.vehicleparking.bikely;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingState;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class BikelyUpdaterTest {

  @Test
  void parseBikeBoxes() {
    var url = "file:src/ext-test/resources/vehicleparking/bikely/bikely.json";
    var timeZone = ZoneId.of("Europe/Oslo");
    var parameters = new BikelyUpdaterParameters(
      "",
      url,
      "bikely",
      Duration.ofSeconds(30),
      Map.of(),
      timeZone
    );
    var updater = new BikelyUpdater(parameters);

    assertTrue(updater.update());
    var parkingLots = updater.getUpdates();

    assertEquals(100, parkingLots.size());

    var first = parkingLots.get(0);
    assertEquals(new FeedScopedId("bikely", "164"), first.getId());
    assertEquals("Husebybadet", first.getName().toString());
    assertFalse(first.hasAnyCarPlaces());
    assertTrue(first.hasBicyclePlaces());
    assertNull(first.getCapacity());

    var last = parkingLots.get(99);
    assertEquals("Hamar Stasjon", last.getName().toString());
    assertNull(last.getOpeningHours());
    assertFalse(last.hasAnyCarPlaces());
    assertFalse(last.hasWheelchairAccessibleCarPlaces());

    var closed = parkingLots
      .stream()
      .map(VehicleParking::getState)
      .filter(x -> x == VehicleParkingState.TEMPORARILY_CLOSED)
      .count();
    assertEquals(2, closed);
  }
}
