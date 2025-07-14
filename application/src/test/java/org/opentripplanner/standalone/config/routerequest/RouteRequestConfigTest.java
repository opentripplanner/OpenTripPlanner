package org.opentripplanner.standalone.config.routerequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.routing.api.request.StreetMode;

class RouteRequestConfigTest {

  @Test
  public void testWheelchairAccessibility() {
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
    assertTrue(subject.journey().wheelchair());

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

  @Test
  public void testAccessEgressPenalty() {
    var nodeAdapter = newNodeAdapterForTest(
      """
         {
      "accessEgress": {
          "penalty": {
       "FLEXIBLE" : { "timePenalty": "2m + 1.1t", "costFactor": 1.7 },
           "CAR" : { "timePenalty": "0s + 4t" }
          }
      }
         }
         """
    );

    var subject = RouteRequestConfig.mapRouteRequest(nodeAdapter);
    var streetPreferences = subject.preferences().street();

    assertEquals(
      "(timePenalty: 2m + 1.10 t, costFactor: 1.70)",
      streetPreferences.accessEgress().penalty().valueOf(StreetMode.FLEXIBLE).toString()
    );
    assertEquals(
      "(timePenalty: 0s + 4.0 t)",
      streetPreferences.accessEgress().penalty().valueOf(StreetMode.CAR).toString()
    );
  }

  @ParameterizedTest
  @ValueSource(strings = { "99", "\"99s\"", "\"1m39s\"", "\"PT1m39s\"" })
  public void transferSlackAsIntOrDuration(String input) {
    var slack = mapSlack(input);
    assertEquals(Duration.ofSeconds(99), slack);
  }

  private static Duration mapSlack(String input) {
    var nodeAdapter = newNodeAdapterForTest(
      """
      {
        "transferSlack": %s
      }
      """.formatted(input)
    );

    var subject = RouteRequestConfig.mapRouteRequest(nodeAdapter);
    return subject.preferences().transfer().slack();
  }
}
