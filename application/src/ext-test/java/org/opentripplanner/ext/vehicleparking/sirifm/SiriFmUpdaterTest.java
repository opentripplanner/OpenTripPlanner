package org.opentripplanner.ext.vehicleparking.sirifm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.test.support.ResourceLoader;

class SiriFmUpdaterTest {

  @Test
  void parse() {
    var uri = ResourceLoader.of(this).uri("siri-fm.xml");
    var parameters = new SiriFmUpdaterParameters(
      "noi",
      uri,
      "noi",
      Duration.ofSeconds(30),
      HttpHeaders.empty()
    );
    var updater = new SiriFmDataSource(parameters);
    updater.update();
    var updates = updater.getUpdates();

    assertEquals(4, updates.size());
  }
}
