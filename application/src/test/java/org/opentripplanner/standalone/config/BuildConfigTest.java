package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.framework.application.OtpFileNames.BUILD_CONFIG_FILENAME;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeForTest;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeFromResource;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.ext.fares.impl.DefaultFareServiceFactory;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

class BuildConfigTest {

  private static final String SOURCE = "BuildConfigTest";

  /**
   * Test that the build-config.json example used in documentation is valid.
   */
  @Test
  void validateExample() {
    var node = jsonNodeFromResource("standalone/config/" + BUILD_CONFIG_FILENAME);

    // Setup so we get access to the NodeAdapter
    var a = new NodeAdapter(node, SOURCE);
    var c = new BuildConfig(a, false);

    // Test for unused parameters
    var buf = new StringBuilder();
    a.logAllWarnings(m -> buf.append("\n").append(m));
    if (!buf.isEmpty()) {
      fail(buf.toString());
    }
  }

  @Test
  public void boardingLocationRefs() {
    var node = jsonNodeForTest("{ 'boardingLocationTags' : ['a-ha', 'royksopp'] }");

    var subject = new BuildConfig(node, "Test", false);

    assertEquals(Set.of("a-ha", "royksopp"), subject.boardingLocationTags);
  }

  @Test
  public void fareService() {
    var node = jsonNodeForTest("{ 'fares' : \"highestFareInFreeTransferWindow\" }");
    var conf = new BuildConfig(node, "Test", false);
    assertInstanceOf(JsonNode.class, conf.fareConfig);
  }
}
