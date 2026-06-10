package org.opentripplanner.ext.vehicleparking.bikely;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingState;
import org.opentripplanner.test.support.ResourceLoader;

public class BikelyUpdaterTest {

  @Test
  void parseBikeBoxes() {
    var uri = ResourceLoader.of(this).uri("bikely.json");
    var parameters = new BikelyUpdaterParameters(
      "",
      uri,
      "bikely",
      Duration.ofSeconds(30),
      HttpHeaders.empty()
    );
    var updater = new BikelyUpdater(parameters);

    assertTrue(updater.update());
    var parkingLots = updater.getUpdates();

    assertEquals(2, parkingLots.size());

    var first = parkingLots.getFirst();
    assertEquals(new FeedScopedId("bikely", "7"), first.getId());
    assertEquals("Gjettum T-banestasjon", first.getName().toString());
    assertFalse(first.hasAnyCarPlaces());
    assertTrue(first.hasBicyclePlaces());

    var availibility = first.getAvailability();
    assertEquals(11, availibility.getBicycleSpaces());

    var capacity = first.getCapacity();
    assertEquals(15, capacity.getBicycleSpaces());
    assertEquals(VehicleParkingState.OPERATIONAL, first.getState());

    parkingLots.forEach(lot -> {
      assertNotNull(lot.getNote().toString());
    });
  }
}
