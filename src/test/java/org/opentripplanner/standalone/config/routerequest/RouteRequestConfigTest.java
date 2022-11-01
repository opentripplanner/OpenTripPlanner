package org.opentripplanner.standalone.config.routerequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.framework.JsonSupport.newNodeAdapterForTest;

import org.junit.jupiter.api.Test;

class RouteRequestConfigTest {

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
          "maxSlope": 0.085,
          "slopeExceededReluctance": 9,
          "inaccessibleStreetReluctance": 10
        }
      }
      """
    );

    var subject = RouteRequestConfig.mapRouteRequest(nodeAdapter);

    var accessibility = subject.preferences().wheelchair();
    assertTrue(subject.wheelchair());

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
    assertEquals(.085, accessibility.maxSlope());
    assertEquals(9.0, accessibility.slopeExceededReluctance());
    assertEquals(10.0, accessibility.inaccessibleStreetReluctance());
  }
}
