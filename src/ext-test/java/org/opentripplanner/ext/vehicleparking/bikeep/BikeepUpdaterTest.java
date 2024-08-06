package org.opentripplanner.ext.vehicleparking.bikeep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.updater.spi.HttpHeaders;

class BikeepUpdaterTest {

  @Test
  void parse() {
    var uri = ResourceLoader.of(this).uri("bikeep.json");
    var parameters = new BikeepUpdaterParameters(
      "bikeep",
      uri,
      "bikeep",
      Duration.ofSeconds(30),
      HttpHeaders.empty()
    );
    var updater = new BikeepUpdater(parameters);
    updater.update();
    var lots = updater.getUpdates();

    assertEquals(9, lots.size());

    lots.forEach(l -> assertNotNull(l.getName()));

    var first = lots.getFirst();
    assertEquals("bikeep:224121", first.getId().toString());
    assertEquals("(60.40593, 4.99634)", first.getCoordinate().toString());
    assertEquals("Ågotnes Terminal", first.getName().toString());
    assertEquals(10, first.getAvailability().getBicycleSpaces());
    assertEquals(10, first.getCapacity().getBicycleSpaces());
    assertEquals(Set.of("FREE", "PRIVATE", "BIKE", "BOOKABLE"), first.getTags());

    var last = lots.getLast();
    assertEquals("bikeep:224111", last.getId().toString());
    assertEquals("(59.88741, 10.5205)", last.getCoordinate().toString());
    assertEquals("Sandvika Storsenter Nytorget", last.getName().toString());
    assertEquals(13, last.getAvailability().getBicycleSpaces());
    assertEquals(15, last.getCapacity().getBicycleSpaces());
  }
}
