package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        GraphBuildParameters parameters = new ConfigLoader(tempDir).loadBuildConfig();

        // then:
        assertTrue(parameters.areaVisibility);
    }

    @Test
    public void loadRouterConfig() throws IOException {
        // Given:
        File file = new File(tempDir, ROUTER_CONFIG_FILENAME);
        FileUtils.write(file, "{key-a : 12}", UTF_8);

        // when:
        JsonNode node = new ConfigLoader(tempDir).loadRouterConfig();

        // then:
        assertEquals(12, node.path("key-a").asInt());
    }

    @Test
    public void whenFileDoNotExistExpectMissingNode() {
        // when: ruter-config.json do not exist
        JsonNode res = new ConfigLoader(tempDir).loadRouterConfig();

        // then: expect missing node
        assertTrue(res.toString(), res.isMissingNode());
    }

    @Test
    public void parseJsonString() {
        // when:
        JsonNode node = ConfigLoader.fromString("{key:\"value\"}", "JSON-STRING");

        // then:
        assertEquals("value", node.path("key").asText());
    }

    @Test
    public void testResolveEnvironmentVariables() {
        // Given: a environment variable (with a alphanumeric name less then 20 characters)
        Map.Entry<String, String> envVar = System.getenv().entrySet()
                .stream()
                .filter(e -> e.getValue().matches("\\w{1,20}"))
                .findFirst()
                .orElse(null);

        String eKey = "key-not-found";
        String eValue = "${" + eKey + "}";

        if(envVar != null) {
            eKey = envVar.getKey();
            eValue = envVar.getValue();
        }

        String json = json(
                "{",
                "  // A comment",
                "  key-a: '${not-existing-env-variable}',",
                "  'key-b': '${" + eKey + "}'",
                "}"
        );

        JsonNode node = ConfigLoader.fromString(json, "test_resolveEnvironmentVariables");

        assertEquals("${not-existing-env-variable}", node.path("key-a").asText(null));
        assertEquals(eKey + " = " + eValue, eValue, node.path("key-b").asText(null));
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

    private static String json(String ... lines) {
        return String.join("\n", lines).replace('\'', '\"');
    }
}