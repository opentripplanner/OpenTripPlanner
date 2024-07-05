package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeForTest;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeFromResource;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

class RouterConfigDocTest {

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
  void testSemanticValidation() {
    // apiProcessingTimeout must be greater then streetRoutingTimeout
    var root = createNodeAdaptor(
      """
      {
        server: { apiProcessingTimeout : "1s" },
        routingDefaults: { streetRoutingTimeout: "17s" }
      }
      """
    );
    //
    assertThrows(OtpAppException.class, () -> new RouterConfig(root, false));
  }

  private static NodeAdapter createNodeAdaptor(String jsonText) {
    return new NodeAdapter(jsonNodeForTest(jsonText), "Test");
  }
}
