package org.opentripplanner.ext.fares;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.impl.HighestFareInFreeTransferWindowFareService;
import org.opentripplanner.standalone.config.JsonSupport;

class FaresConfigurationTest extends JsonSupport {

  @Test
  void houston() {
    var node = jsonNodeForTest("\"highestFareInFreeTransferWindow\"");
    var service = FaresConfiguration.fromConfig(node).makeFareService();
    assertSame(HighestFareInFreeTransferWindowFareService.class, service.getClass());
  }

  @Test
  void houstonWithParams() {
    var node = jsonNodeForTest(
      """
        {
          "type": "highestFareInFreeTransferWindow",
          "freeTransferWindow": "30m",
          "analyzeInterlinedTransfers": true
        }
      """
    );
    var service = FaresConfiguration.fromConfig(node).makeFareService();
    assertInstanceOf(HighestFareInFreeTransferWindowFareService.class, service);

    var houston = (HighestFareInFreeTransferWindowFareService) service;
    assertEquals(Duration.ofMinutes(30), houston.freeTransferWindow());
    assertTrue(houston.analyzeInterlinedTransfers());
  }
}
