package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeForTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.framework.application.OtpAppException;

class DebugUiConfigTest {

  @Test
  void vectorTilesBasePathDefaultsWhenNotSet() {
    var config = new DebugUiConfig(jsonNodeForTest("{}"), "TEST", true);
    assertEquals("/otp/debug/vectortiles/", config.vectorTilesBasePath());
  }

  @Test
  void vectorTilesBasePathIsReadFromConfig() {
    var config = new DebugUiConfig(
      jsonNodeForTest("{ 'vectorTilesBasePath': '/gateway/otp/debug/vectortiles' }"),
      "TEST",
      true
    );
    assertEquals("/gateway/otp/debug/vectortiles", config.vectorTilesBasePath());
  }

  @ParameterizedTest
  // the tab is escaped for the JSON text, which does not allow a literal one in a string
  @ValueSource(strings = { "", " ", "  \\t" })
  void blankVectorTilesBasePathIsRejected(String blank) {
    var json = jsonNodeForTest("{ 'vectorTilesBasePath': '%s' }".formatted(blank));
    var ex = assertThrows(OtpAppException.class, () -> new DebugUiConfig(json, "TEST", true));
    assertTrue(ex.getMessage().contains("vectorTilesBasePath"), ex.getMessage());
    assertTrue(ex.getMessage().contains("must not be blank"), ex.getMessage());
  }
}
