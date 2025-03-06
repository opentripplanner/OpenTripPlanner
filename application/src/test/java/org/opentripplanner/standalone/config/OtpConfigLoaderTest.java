package org.opentripplanner.standalone.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.framework.application.OtpFileNames.BUILD_CONFIG_FILENAME;
import static org.opentripplanner.framework.application.OtpFileNames.ROUTER_CONFIG_FILENAME;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.config.framework.file.ConfigFileLoader;
import org.opentripplanner.utils.lang.StringUtils;

public class OtpConfigLoaderTest {

  private File tempDir;

  @BeforeEach
  public void setUp() throws IOException {
    tempDir = Files.createTempDirectory("OtpDataStoreTest-").toFile();
  }

  @AfterEach
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void tearDown() {
    tempDir.delete();
  }

  @Test
  public void loadBuildConfig() throws IOException {
    // Given:
    String json = "{areaVisibility:true}";

    File file = new File(tempDir, BUILD_CONFIG_FILENAME);
    Files.writeString(file.toPath(), json, UTF_8);

    // when:
    BuildConfig parameters = new OtpConfigLoader(tempDir).loadBuildConfig();

    // then:
    assertTrue(parameters.areaVisibility);
  }

  @Test
  public void loadRouterConfig() throws IOException {
    // Given:
    File file = new File(tempDir, ROUTER_CONFIG_FILENAME);
    Files.writeString(file.toPath(), "{ server: { apiProcessingTimeout: \"13s\" } }", UTF_8);

    // when:
    RouterConfig params = new OtpConfigLoader(tempDir).loadRouterConfig();

    // then:
    assertEquals(13, params.server().apiProcessingTimeout().toSeconds());
  }

  @Test
  public void whenFileDoNotExistExpectMissingNode() {
    // when: ruter-config.json do not exist
    RouterConfig res = new OtpConfigLoader(tempDir).loadRouterConfig();

    // then: expect missing node
    assertTrue(res.isDefault(), "Expect default value");
  }

  /**
   * Test replacing environment variables in JSON config. The {@link OtpConfigLoader} should replace
   * placeholders like '${ENV_NAME}' when converting a JSON string to a node tree.
   * <p>
   * This test pick a random system environment variable and insert it into the JSON string to be
   * able to test the replace functionality. This is necessary to avoid changing the system
   * environment variables and to apply this test on the {@link OtpConfigLoader} level.
   */
  @Test
  public void testReplacementOfEnvironmentVariables() {
    // Given: Search for a environment variable name containing only alphanumeric characters
    //        and a value with less than 30 characters (avoid long values like paths for
    //        readability). We will use this to insert it in the JSON and later see if the
    //        ConfigLoader is able to replace the placeholder with the expected value.
    Map.Entry<String, String> envVar = System.getenv()
      .entrySet()
      .stream()
      .filter(e -> e.getKey().matches("\\w+") && e.getValue().length() < 30)
      .findFirst()
      .orElse(null);

    if (envVar == null) {
      fail("No environment variable matching '\\w+' found.");
    }

    String eName = envVar.getKey();
    String expectedValue = envVar.getValue();

    // Create JSON with the environment variable inserted
    String json = json("{  'key': '${" + eName + "}', 'key2':'${" + eName + "}' }");

    // When: parse JSON
    JsonNode node = ConfigFileLoader.nodeFromString(json, "test");

    // Then: verify that the JSON node have the expected value
    String actualValue = node.path("key").asText(null);

    assertEquals(expectedValue, actualValue);
  }

  private static String json(String text) {
    return StringUtils.quoteReplace(text);
  }
}
