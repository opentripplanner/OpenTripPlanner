package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.standalone.config.framework.JsonSupport.jsonNodeForTest;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.config.framework.NodeAdapter;
import org.opentripplanner.util.OtpAppException;

class RouterConfigTest {

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
