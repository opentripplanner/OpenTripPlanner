package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.util.OtpAppException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConfigLoaderTest {
    private static final String OTP_CONFIG_FILENAME = "otp-config.json";
    private static final String BUILD_CONFIG_FILENAME = "build-config.json";
    private static final String ROUTER_CONFIG_FILENAME = "router-config.json";
    private static final String UTF_8 = "UTF-8";
    private File tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("OtpDataStoreTest-").toFile();
    }

    @After
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void tearDown() {
        tempDir.delete();
    }


    @Test
    public void isConfigFile() {
        assertTrue(ConfigLoader.isConfigFile(OTP_CONFIG_FILENAME));
        assertTrue(ConfigLoader.isConfigFile(BUILD_CONFIG_FILENAME));
        assertTrue(ConfigLoader.isConfigFile(ROUTER_CONFIG_FILENAME));
        assertFalse(ConfigLoader.isConfigFile("not-config.json"));
    }

    @Test
    public void loadBuildConfig() throws IOException {
        // Given:
        String json = "{areaVisibility:true}";

        File file = new File(tempDir, BUILD_CONFIG_FILENAME);
        FileUtils.write(file, json, UTF_8);

        // when:
        BuildConfig parameters = new ConfigLoader(tempDir).loadBuildConfig();

        // then:
        assertTrue(parameters.areaVisibility);
    }

    @Test
    public void loadRouterConfig() throws IOException {
        // Given:
        File file = new File(tempDir, ROUTER_CONFIG_FILENAME);
        FileUtils.write(file, "{requestLogFile : \"aFile.txt\"}", UTF_8);

        // when:
        RouterConfig params = new ConfigLoader(tempDir).loadRouterConfig();

        // then:
        assertEquals("aFile.txt", params.requestLogFile());
    }

    @Test
    public void whenFileDoNotExistExpectMissingNode() {
        // when: ruter-config.json do not exist
        RouterConfig res = new ConfigLoader(tempDir).loadRouterConfig();

        // then: expect missing node
        assertNull("Expect deafult value(null)", res.requestLogFile());
    }

    @Test
    public void parseJsonString() {
        // when:
        JsonNode node = ConfigLoader.nodeFromString("{key:\"value\"}", "JSON-STRING");

        // then:
        assertEquals("value", node.path("key").asText());
    }

    /**
     * Test replacing environment variables in JSON config. The {@link ConfigLoader} should replace
     * placeholders like '${ENV_NAME}' when converting a JSON string to a node tree.
     * <p>
     * This test pick a random system environment variable and insert it into the JSON string to
     * be able to test the replace functionality. This is necessary to avoid changing the system
     * environment variables and to apply this test on the {@link ConfigLoader} level.
     */
    @Test
    public void testReplacementOfEnvironmentVariables() {
        // Given: Search for a environment variable name containing only alphanumeric characters
        //        and a value with less than 30 characters (avoid long values like paths for
        //        readability). We will use this to insert it in the JSON and later see if the
        //        ConfigLoader is able to replace the placeholder with the expected value.
        Map.Entry<String, String> envVar = System.getenv().entrySet()
                .stream()
                .filter(e ->
                        e.getKey().matches("\\w+") && e.getValue().length() < 30
                )
                .findFirst()
                .orElse(null);

        if(envVar == null) {
            fail("No environment variable matching '\\w+' found.");
        }

        String eName = envVar.getKey();
        String expectedValue = envVar.getValue();

        // Create JSON with the environment variable inserted
        String json = json("{  'key': '${" + eName + "}', 'key2':'${" + eName + "}' }");

        // When: parse JSON
        JsonNode node = ConfigLoader.nodeFromString(json, "test");

        // Then: verify that the JSON node have the expected value
        String actualValue = node.path("key").asText(null);

        assertEquals(expectedValue, actualValue);
    }

    /**
     * Test replacing environment variables in config fails on a unknown environment variable.
     */
    @Test(expected = OtpAppException.class)
    public void testMissingEnvironmentVariable() {
        ConfigLoader.nodeFromString(
                json("{ key: '${none_existing_env_variable}' }"),
                "test"
        );
    }

    @Test
    public void configFailsIfBaseDirectoryDoesNotExist() {
        File cfgDir = new File(tempDir, "cfg");

        try {
            new ConfigLoader(cfgDir);
            fail("Expected to fail!");
        }
        catch (Exception e) {
            assertTrue(e.getMessage(), e.getMessage().contains(cfgDir.getName()));
        }
    }

    @Test
    public void configFailsIfConfigDirIsAFile() throws IOException {
        File file = new File(tempDir, "AFile.txt");
        FileUtils.write(file, "{}", UTF_8);

        try {
            new ConfigLoader(file);
            fail("Expected to fail!");
        }
        catch (Exception e) {
            assertTrue(e.getMessage(), e.getMessage().contains(file.getName()));
        }
    }

    private static String json(String text) {
        return text.replace('\'', '\"');
    }
}