package org.opentripplanner.ext.vehicleparking.noi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.updater.spi.HttpHeaders;

class NoiUpdaterTest {

  @Test
  void parse() {
    var uri = ResourceLoader.of(this).uri("stations.json");
    var parameters = new NoiUpdaterParameters(
      "noi",
      uri,
      "noi",
      Duration.ofSeconds(30),
      HttpHeaders.empty()
    );
    var updater = new NoiUpdater(parameters);
    updater.update();
    var lots = updater.getUpdates();

    assertEquals(14, lots.size());

    lots.forEach(l -> assertNotNull(l.getName()));

    var first = lots.getFirst();
    assertEquals("noi:105", first.getId().toString());
    assertEquals("(46.49817, 11.35726)", first.getCoordinate().toString());
    assertEquals("P05 - Laurin", first.getName().toString());
    assertEquals(57, first.getAvailability().getCarSpaces());
    assertEquals(90, first.getCapacity().getCarSpaces());

    var last = lots.getLast();
    assertEquals(
      "noi:935af00d-aa5f-eb11-9889-501ac5928d31-0.8458736393052522",
      last.getId().toString()
    );
    assertEquals("(46.5057, 11.3395)", last.getCoordinate().toString());
    assertEquals(
      "Parksensoren Bozen - PNI Parksensor Nr.10 Commissariato - Viale Eugenio di savoia",
      last.getName().toString()
    );
    assertEquals(0, last.getAvailability().getCarSpaces());
    assertEquals(1, last.getCapacity().getCarSpaces());
  }
}
