package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.standalone.config.JsonSupport.jsonNodeForTest;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RouterConfigTest {

  @Test
  void parseStreetRoutingTimeout() {
    var DEFAULT_TIMEOUT = RouterConfig.DEFAULT.streetRoutingTimeout();
    NodeAdapter c;

    // Fall back to default 5 seconds
    c = new NodeAdapter(jsonNodeForTest("{}"), "Test");
    assertEquals(DEFAULT_TIMEOUT, RouterConfig.parseStreetRoutingTimeout(c));

    // New format: 33 seconds
    c = new NodeAdapter(jsonNodeForTest("{streetRoutingTimeout: '33s'}"), "Test");
    assertEquals(Duration.ofSeconds(33), RouterConfig.parseStreetRoutingTimeout(c));

    // Old format: 3.7 seconds
    c = new NodeAdapter(jsonNodeForTest("{streetRoutingTimeout: 3.7}"), "Test");
    assertEquals(Duration.ofMillis(3700), RouterConfig.parseStreetRoutingTimeout(c));

    // Illegal format
    c = new NodeAdapter(jsonNodeForTest("{streetRoutingTimeout: 'Hi'}"), "Test");
    assertEquals(DEFAULT_TIMEOUT, RouterConfig.parseStreetRoutingTimeout(c));
  }
}
