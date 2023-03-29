package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeForTest;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeFromResource;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

class RouterConfigTest {

  private static final String SOURCE = "RouterConfigTest";

  /**
   * Test that the router-config.json example used in documentation is valid.
   */
  @Test
  void validateExample() {
    var node = jsonNodeFromResource("standalone/config/router-config.json");

    // Setup so we get access to the NodeAdapter
    var a = new NodeAdapter(node, SOURCE);
    var c = new RouterConfig(a, false);

    // Test for unused parameters
    var buf = new StringBuilder();
    a.logAllWarnings(m -> buf.append("\n").append(m));
    if (!buf.isEmpty()) {
      fail(buf.toString());
    }
  }

  @Test
  void parseStreetRoutingTimeout() {
    var DEFAULT_TIMEOUT = RouterConfig.DEFAULT
      .routingRequestDefaults()
      .preferences()
      .street()
      .routingTimeout();
    NodeAdapter c;

    // Fall back to default 5 seconds
    c = createNodeAdaptor("{}");
    assertEquals(DEFAULT_TIMEOUT, RouterConfig.parseStreetRoutingTimeout(c, DEFAULT_TIMEOUT));

    // New format: 33 seconds
    c = createNodeAdaptor("{streetRoutingTimeout: '33s'}");
    assertEquals(
      Duration.ofSeconds(33),
      RouterConfig.parseStreetRoutingTimeout(c, DEFAULT_TIMEOUT)
    );
  }

  @Test
  void parseStreetRoutingTimeoutWithIllegalFormat() {
    Duration defaultTimeout = Duration.ZERO;
    final var c = createNodeAdaptor("{streetRoutingTimeout: 'Hi'}");
    assertThrows(
      OtpAppException.class,
      () -> RouterConfig.parseStreetRoutingTimeout(c, defaultTimeout)
    );
  }

  private static NodeAdapter createNodeAdaptor(String jsonText) {
    return new NodeAdapter(jsonNodeForTest(jsonText), "Test");
  }
}
