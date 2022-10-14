package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.standalone.config.framework.JsonSupport.jsonNodeFromPath;

import java.nio.file.Path;
import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.test.support.FilePatternSource;

public class ExampleConfigTest {

  @FilePatternSource(pattern = "docs/examples/**/router-config.json")
  @ParameterizedTest(name = "Check validity of {0}")
  void routerConfig(Path filename) {
    testConfig(filename, a -> new RouterConfig(a, true));
  }

  @FilePatternSource(pattern = "docs/examples/**/build-config.json")
  @ParameterizedTest(name = "Check validity of {0}")
  void buildConfig(Path filename) {
    testConfig(filename, a -> new BuildConfig(a, true));
  }

  private void testConfig(Path filename, Consumer<NodeAdapter> buildConfig) {
    var node = jsonNodeFromPath(filename);
    var a = new NodeAdapter(node, getClass().getSimpleName());
    buildConfig.accept(a);

    // Test for unused parameters
    var buf = new StringBuilder();
    a.logAllUnusedParameters(m -> buf.append("\n").append(m));
    if (!buf.isEmpty()) {
      fail(buf.toString());
    }
  }
}
