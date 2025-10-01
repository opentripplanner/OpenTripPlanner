package org.opentripplanner.ext.fares;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.fares.impl.gtfs.CombinedInterlinedLegsFareService.CombinationMode.SAME_ROUTE;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.impl.HighestFareInFreeTransferWindowFareService;
import org.opentripplanner.ext.fares.impl.gtfs.CombinedInterlinedLegsFareService;
import org.opentripplanner.standalone.config.framework.json.JsonSupport;

class FaresConfigurationTest extends JsonSupport {

  @Test
  void houston() {
    var node = jsonNodeForTest("\"highestFareInFreeTransferWindow\"");
    var service = FaresConfiguration.fromConfig(node).makeFareService();
    assertInstanceOf(HighestFareInFreeTransferWindowFareService.class, service);
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

  @Test
  void combineInterlinedLegs() {
    var node = jsonNodeForTest("\"combine-interlined-legs\"");
    var service = FaresConfiguration.fromConfig(node).makeFareService();
    assertInstanceOf(CombinedInterlinedLegsFareService.class, service);
  }

  @Test
  void combineInterlinedLegsWithParams() {
    var node = jsonNodeForTest(
      """
        {
          "type": "combine-interlined-legs",
          "mode": "SAME_ROUTE"
        }
      """
    );
    var service = FaresConfiguration.fromConfig(node).makeFareService();
    assertInstanceOf(CombinedInterlinedLegsFareService.class, service);

    var s = (CombinedInterlinedLegsFareService) service;
    assertEquals(SAME_ROUTE, s.mode());
  }
}
