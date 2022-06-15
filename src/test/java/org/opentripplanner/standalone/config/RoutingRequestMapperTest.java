package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.JsonSupport.newNodeAdapterForTest;

import org.junit.jupiter.api.Test;

class RoutingRequestMapperTest {

  @Test
  public void loadFromJson() {
    var nodeAdapter = newNodeAdapterForTest(
      """
      {
        "wheelchairAccessibility": {
          "enabled": true,
          "trip": {
            "onlyConsiderAccessible": false,
            "unknownCost": 1,
            "inaccessibleCost": 2
          },
          "stop": {
            "onlyConsiderAccessible": false,
            "unknownCost": 3,
            "inaccessibleCost": 4
          },
          "elevator": {
            "onlyConsiderAccessible": false,
            "unknownCost": 5,
            "inaccessibleCost": 6
          },
          "stairsReluctance": 7,
          "maxSlope": 8,
          "slopeExceededReluctance": 9,
          "inaccessibleStreetReluctance": 10
        }
      }
      """
    );

    var subject = RoutingRequestMapper.mapRoutingRequest(nodeAdapter);

    var accessibility = subject.wheelchairAccessibility;
    assertTrue(accessibility.enabled());

    assertFalse(accessibility.trip().onlyConsiderAccessible());
    assertEquals(1, accessibility.trip().unknownCost());
    assertEquals(2, accessibility.trip().inaccessibleCost());

    assertFalse(accessibility.stop().onlyConsiderAccessible());
    assertEquals(3, accessibility.stop().unknownCost());
    assertEquals(4, accessibility.stop().inaccessibleCost());

    assertFalse(accessibility.elevator().onlyConsiderAccessible());
    assertEquals(5, accessibility.elevator().unknownCost());
    assertEquals(6, accessibility.elevator().inaccessibleCost());

    assertFalse(accessibility.elevator().onlyConsiderAccessible());
    assertEquals(5, accessibility.elevator().unknownCost());
    assertEquals(6, accessibility.elevator().inaccessibleCost());

    assertEquals(7.0, accessibility.stairsReluctance());
    assertEquals(8.0, accessibility.maxSlope());
    assertEquals(9.0, accessibility.slopeExceededReluctance());
    assertEquals(10.0, accessibility.inaccessibleStreetReluctance());
  }
}
