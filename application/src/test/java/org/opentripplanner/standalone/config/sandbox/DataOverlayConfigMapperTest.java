package org.opentripplanner.standalone.config.sandbox;

import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.jsonNodeFromResource;

import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

class DataOverlayConfigMapperTest {

  @Test
  void validateExample() {
    var node = jsonNodeFromResource("standalone/config/sandbox/build-config-data-overlay.json");

    // Setup so we get access to the NodeAdapter
    var a = new NodeAdapter(node, DataOverlayConfigMapperTest.class.getSimpleName());

    DataOverlayConfigMapper.map(a, "dataOverlay");

    // Test for unused parameters
    var buf = new StringBuilder();
    a.logAllWarnings(m -> buf.append("\n").append(m));
    if (!buf.isEmpty()) {
      fail(buf.toString());
    }
  }
}
