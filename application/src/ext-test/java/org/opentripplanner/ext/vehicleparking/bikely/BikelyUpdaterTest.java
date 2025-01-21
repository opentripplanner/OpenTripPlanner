package org.opentripplanner.ext.vehicleparking.bikely;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Locale;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingState;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.basic.Locales;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.spi.HttpHeaders;

public class BikelyUpdaterTest {

  @Test
  @Disabled
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

    assertEquals(8, parkingLots.size());

    var first = parkingLots.getFirst();
    assertEquals(new FeedScopedId("bikely", "7"), first.getId());
    assertEquals("Gjettum T-banestasjon", first.getName().toString());
    assertFalse(first.hasAnyCarPlaces());
    assertTrue(first.hasBicyclePlaces());

    assertEquals(
      "First 12 hour(s) is NOK0.00, afterwards NOK10.00 per 1 hour(s)",
      first.getNote().toString(Locale.ROOT)
    );
    // This test fails in the entur ci pipline
    assertEquals(
      "Første 12 time(r) er kr 0,00. Deretter kr 10,00 per 1 time(r)",
      first.getNote().toString(Locales.NORWEGIAN_BOKMAL)
    );
    assertEquals(
      "Første 12 time(r) er kr 0,00. Deretter kr 10,00 per 1 time(r)",
      first.getNote().toString(Locales.NORWAY)
    );
    var availibility = first.getAvailability();
    assertEquals(8, availibility.getBicycleSpaces());

    var capacity = first.getCapacity();
    assertEquals(10, capacity.getBicycleSpaces());

    var freeParkingLots = parkingLots.get(2);
    assertEquals(
      "First 12 hour(s) is NOK0.00, afterwards NOK10.00 per 1 hour(s)",
      freeParkingLots.getNote().toString(Locale.ENGLISH)
    );

    assertEquals(VehicleParkingState.OPERATIONAL, first.getState());

    parkingLots.forEach(lot -> {
      assertNotNull(lot.getNote().toString());
    });
  }
}
