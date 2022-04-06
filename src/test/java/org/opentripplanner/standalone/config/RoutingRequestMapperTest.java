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
                            "trips": {
                              "onlyConsiderAccessible": false,
                              "unknownCost": 1,
                              "inaccessibleCost": 2
                            },
                            "stops": {
                              "onlyConsiderAccessible": false,
                              "unknownCost": 3,
                              "inaccessibleCost": 4
                            }
                          }
                        }
                        """
    );

    var subject = RoutingRequestMapper.mapRoutingRequest(nodeAdapter);

    var accessibility = subject.accessibilityRequest;
    assertTrue(accessibility.enabled());

    assertFalse(accessibility.trips().onlyConsiderAccessible());
    assertEquals(1, accessibility.trips().unknownCost());
    assertEquals(2, accessibility.trips().inaccessibleCost());

    assertFalse(accessibility.stops().onlyConsiderAccessible());
    assertEquals(3, accessibility.stops().unknownCost());
    assertEquals(4, accessibility.stops().inaccessibleCost());
  }
}
