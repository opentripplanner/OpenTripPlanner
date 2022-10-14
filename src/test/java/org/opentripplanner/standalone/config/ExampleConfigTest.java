package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.standalone.config.framework.JsonSupport.jsonNodeFromPath;

import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.test.support.FilePatternSource;

public class ExampleConfigTest {

  @FilePatternSource(pattern = "docs/examples/**/router-config.json")
  @ParameterizedTest(name = "Check validity of {0}")
  void testExamples(Path filename) {
    var node = jsonNodeFromPath(filename);

    // Setup so we get access to the NodeAdapter
    var a = new NodeAdapter(node, getClass().getSimpleName());
    new RouterConfig(a, false);

    // Test for unused parameters
    var buf = new StringBuilder();
    a.logAllUnusedParameters(m -> buf.append("\n").append(m));
    if (!buf.isEmpty()) {
      fail(buf.toString());
    }
  }
}
