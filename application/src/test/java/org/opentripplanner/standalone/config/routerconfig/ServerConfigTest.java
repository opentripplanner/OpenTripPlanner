package org.opentripplanner.standalone.config.routerconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeForTest;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
  }

  @Test
  void parseServer() {
    var root = createNodeAdaptor(
      """
      {
        server: {
          apiProcessingTimeout : "7s",
          traceParameters : [
            {
              httpRequestHeader : "IN-ID",
              httpResponseHeader : "OUT-ID",
              logKey : "LOG-ID",
              generateIdIfMissing: true
            }
          ]
        }
      }
      """
    );
    var config = new ServerConfig("server", root);

    assertEquals(Duration.ofSeconds(7), config.apiProcessingTimeout());
    assertFalse(config.traceParameters().isEmpty());
    var traceParameters = config.traceParameters().get(0);
    assertEquals("IN-ID", traceParameters.httpRequestHeader());
    assertEquals("OUT-ID", traceParameters.httpResponseHeader());
    assertEquals("LOG-ID", traceParameters.logKey());
    assertTrue(traceParameters.generateIdIfMissing());
  }

  static List<String> parseIncompleteServerConfigTestCases() {
    return List.of(
      "{ httpRequestHeader : \"a\", generateIdIfMissing : true }",
      "{ httpResponseHeader : \"a\", logKey : \"a\" }",
      "{ }"
    );
  }

  @ParameterizedTest
  @MethodSource("parseIncompleteServerConfigTestCases")
  void parseIncompleteServerConfig(String traceParameterJson) {
    var root = createNodeAdaptor(
      """
      {
        server: {
          traceParameters : [
            OBJECT
          ]
        }
      }
      """.replace("OBJECT", traceParameterJson)
    );
    assertThrows(IllegalArgumentException.class, () -> new ServerConfig("server", root));
  }

  private static NodeAdapter createNodeAdaptor(String jsonText) {
    return new NodeAdapter(jsonNodeForTest(jsonText), "Test");
  }
}
