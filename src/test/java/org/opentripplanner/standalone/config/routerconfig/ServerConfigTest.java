package org.opentripplanner.standalone.config.routerconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeForTest;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class ServerConfigTest {

  @Test
  void foo() {}

  @Test
  void parseServerDefaults() {
    var SERVER_DEFAULT = RouterConfig.DEFAULT.server();
    var root = createNodeAdaptor("{}");
    var config = new ServerConfig("server", root);

    assertEquals(SERVER_DEFAULT.apiProcessingTimeout(), config.apiProcessingTimeout());
    assertEquals(SERVER_DEFAULT.requestLogFile(), config.requestLogFile());
  }

  @Test
  void parseServer() {
    var root = createNodeAdaptor(
      """
      {
        server: {
          requestLogFile : "req-log-file",
          apiProcessingTimeout : "7s"
        }
      }
      """
    );
    var config = new ServerConfig("server", root);

    assertEquals(Duration.ofSeconds(7), config.apiProcessingTimeout());
    assertEquals("req-log-file", config.requestLogFile());
  }

  private static NodeAdapter createNodeAdaptor(String jsonText) {
    return new NodeAdapter(jsonNodeForTest(jsonText), "Test");
  }
}
