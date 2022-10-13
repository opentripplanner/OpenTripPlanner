package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.standalone.config.framework.JsonSupport.jsonNodeForTest;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.util.OtpAppException;

class RouterConfigTest {

  private static final String SOURCE = "RouterConfigTest";

  @Test
  void completeConfigTest() {
    var node = jsonNodeForTest(
      """
      {
        "updaters" : [ {
          "type" : "vehicle-positions",
          "sourceType" : "gtfs-rt-http",
          "url" : "https://www3.septa.org/gtfsrt/septarail-pa-us/Vehicle/rtVehiclePosition.pb",
          "feedId" : "septa-rail",
          "frequencySec" : 60
        }, {
          "type" : "vehicle-rental",
          "sourceType" : "gbfs",
          "frequencySec" : 60,
          "url" : "https://gbfs.bcycle.com/bcycle_indego/gbfs.json"
        } ]
      }
      """
    );

    // Setup so we get access to the NodeAdapter, it is used later in the test
    var a = new NodeAdapter(node, SOURCE);
    var c = new RouterConfig(a, SOURCE, false);

    // Test for unused parameters
    var buf = new StringBuilder();
    a.logAllUnusedParameters(m -> buf.append(m).append("\n"));
    if (buf.length() > 10) {
      System.out.println(buf);
    }
    assertEquals("", buf.toString());
  }

  @Test
  void parseStreetRoutingTimeout() {
    var DEFAULT_TIMEOUT = RouterConfig.DEFAULT.streetRoutingTimeout();
    NodeAdapter c;

    // Fall back to default 5 seconds
    c = createNodeAdaptor("{}");
    assertEquals(DEFAULT_TIMEOUT, RouterConfig.parseStreetRoutingTimeout(c));

    // New format: 33 seconds
    c = createNodeAdaptor("{streetRoutingTimeout: '33s'}");
    assertEquals(Duration.ofSeconds(33), RouterConfig.parseStreetRoutingTimeout(c));
  }

  @Test
  void parseStreetRoutingTimeoutWithIllegalFormat() {
    final var c = createNodeAdaptor("{streetRoutingTimeout: 'Hi'}");
    assertThrows(OtpAppException.class, () -> RouterConfig.parseStreetRoutingTimeout(c));
  }

  private static NodeAdapter createNodeAdaptor(String jsonText) {
    return new NodeAdapter(jsonNodeForTest(jsonText), "Test");
  }
}
