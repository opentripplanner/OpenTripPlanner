package org.opentripplanner.standalone.config.framework.file;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.standalone.config.OtpConfigLoader;
import org.opentripplanner.utils.lang.StringUtils;

class ConfigFileLoaderTest {

  private File tempDir;

  @BeforeEach
  public void setUp() throws IOException {
    tempDir = Files.createTempDirectory("ConfigFileLoaderTest-").toFile();
  }

  @AfterEach
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void tearDown() {
    tempDir.delete();
  }

  @Test
  public void nodeFromString() {
    // when:
    JsonNode node = ConfigFileLoader.nodeFromString("{key:\"value\"}", "JSON-STRING");

    // then:
    assertEquals("value", node.path("key").asText());
  }

  @Test
  void loadFromFile() {}

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

  /**
   * Test replacing environment variables in config fails on a unknown environment variable.
   */
  @Test
  public void testMissingEnvironmentVariable() {
    assertThrows(OtpAppException.class, () ->
      ConfigFileLoader.nodeFromString(json("{ key: '${none_existing_env_variable}' }"), "test")
    );
  }

  @Test
  public void configFailsIfBaseDirectoryDoesNotExist() {
    File cfgDir = new File(tempDir, "cfg");

    assertThrows(
      Exception.class,
      () -> ConfigFileLoader.of().withConfigDir(cfgDir),
      "" + cfgDir.getName()
    );
  }

  @Test
  public void configFailsIfConfigDirIsAFile() throws IOException {
    File file = new File(tempDir, "AFile.txt");
    Files.writeString(file.toPath(), "{}", UTF_8);

    assertThrows(
      Exception.class,
      () -> ConfigFileLoader.of().withConfigDir(file),
      "" + file.getName()
    );
  }

  private static String json(String text) {
    return StringUtils.quoteReplace(text);
  }
}
