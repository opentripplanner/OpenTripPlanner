package org.opentripplanner.standalone.config;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.opentripplanner.standalone.config.framework.JsonSupport;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.project.EnvironmentVariableReplacer;
import org.opentripplanner.test.support.FilePatternSource;

public class ExampleConfigTest {

  Map<String, String> env = Map.ofEntries(
    entry("OTP_GCS_WORK_DIR", "/var/entur"),
    entry("OTP_GCS_BASE_GRAPH_PATH", "/var/entur"),
    entry("OTP_GCS_BUCKET", "/var/entur")
  );

  @FilePatternSource(pattern = "docs/examples/**/router-config.json")
  @ParameterizedTest(name = "Check validity of {0}")
  void routerConfig(Path filename) {
    testConfig(filename, a -> new RouterConfig(a, true));
  }

  @FilePatternSource(
    pattern = { "docs/examples/**/build-config.json", "test/performance/**/build-config.json" }
  )
  @ParameterizedTest(name = "Check validity of {0}")
  void buildConfig(Path filename) {
    testConfig(filename, a -> new BuildConfig(a, true));
  }

  private void testConfig(Path path, Consumer<NodeAdapter> buildConfig) {
    try {
      var json = Files.readString(path);
      var replaced = EnvironmentVariableReplacer.insertEnvironmentVariables(json, json, env);
      var node = JsonSupport.jsonNodeFromString(replaced);
      var a = new NodeAdapter(node, getClass().getSimpleName());
      buildConfig.accept(a);

      // Test for unused parameters
      var buf = new StringBuilder();
      a.logAllUnusedParameters(m -> buf.append("\n").append(m));
      if (!buf.isEmpty()) {
        fail(buf.toString());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
