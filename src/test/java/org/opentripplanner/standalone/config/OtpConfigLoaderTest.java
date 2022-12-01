package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.lang.StringUtils;
import org.opentripplanner.standalone.config.framework.file.ConfigFileLoader;

public class OtpConfigLoaderTest {

  private static final String OTP_CONFIG_FILENAME = "otp-config.json";
  private static final String BUILD_CONFIG_FILENAME = "build-config.json";
  private static final String ROUTER_CONFIG_FILENAME = "router-config.json";
  private static final String UTF_8 = "UTF-8";
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
  public void isConfigFile() {
    assertTrue(OtpConfigLoader.isConfigFile(OTP_CONFIG_FILENAME));
    assertTrue(OtpConfigLoader.isConfigFile(BUILD_CONFIG_FILENAME));
    assertTrue(OtpConfigLoader.isConfigFile(ROUTER_CONFIG_FILENAME));
    assertFalse(OtpConfigLoader.isConfigFile("not-config.json"));
  }

  @Test
  public void loadBuildConfig() throws IOException {
    // Given:
    String json = "{areaVisibility:true}";

    File file = new File(tempDir, BUILD_CONFIG_FILENAME);
    FileUtils.write(file, json, UTF_8);

    // when:
    BuildConfig parameters = new OtpConfigLoader(tempDir).loadBuildConfig();

    // then:
    assertTrue(parameters.areaVisibility);
  }

  @Test
  public void loadRouterConfig() throws IOException {
    // Given:
    File file = new File(tempDir, ROUTER_CONFIG_FILENAME);
    FileUtils.write(file, "{requestLogFile : \"aFile.txt\"}", UTF_8);

    // when:
    RouterConfig params = new OtpConfigLoader(tempDir).loadRouterConfig();

    // then:
    assertEquals("aFile.txt", params.requestLogFile());
  }

  @Test
  public void whenFileDoNotExistExpectMissingNode() {
    // when: ruter-config.json do not exist
    RouterConfig res = new OtpConfigLoader(tempDir).loadRouterConfig();

    // then: expect missing node
    assertNull(res.requestLogFile(), "Expect deafult value(null)");
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
    Map.Entry<String, String> envVar = System
      .getenv()
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
