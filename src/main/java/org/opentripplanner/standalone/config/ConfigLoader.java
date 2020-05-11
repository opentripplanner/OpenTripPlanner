package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.apache.commons.io.IOUtils;
import org.opentripplanner.util.OtpAppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.opentripplanner.util.EnvironmentVariableReplacer.insertEnvironmentVariables;

/**
 * Generic config file loader. This is used to load all configuration files.
 * <p>
 * This class is also provide logging when a config file is loaded. We load and parse config files
 * early to reveal syntax errors without waiting for graph build.
 */
public class ConfigLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigLoader.class);

    private static final String OTP_CONFIG_FILENAME = "otp-config.json";
    private static final String BUILD_CONFIG_FILENAME = "build-config.json";
    private static final String ROUTER_CONFIG_FILENAME = "router-config.json";

    private final ObjectMapper mapper = new ObjectMapper();
    @Nullable
    private final File configDir;
    private final String jsonFallback;

    private ConfigLoader(File configDir, String jsonFallback) {
        this.configDir = configDir;
        this.jsonFallback = jsonFallback;
        assertConfigDirIsADirectory();
        // Configure mapper
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

    /**
     * Create a config loader that load config from the given directory.
     */
    public ConfigLoader(File configDir) {
        this(configDir, null);
    }

    /**
     * Create a config loader that load config from the given input json document.
     * Use it with {@link #loadBuildConfig()} or one of the other load methods to
     * return a configuration for the given input json string.
     */
    public static ConfigLoader fromString(String json) {
        return new ConfigLoader(null, json);
    }

    /**
     * Generic method to parse the given json and return a JsonNode tree. The {@code source} is
     * used to generate a proper error message in case the string is not a proper JSON document.
     */
    public static JsonNode nodeFromString(String json, String source) {
        return new ConfigLoader(null, null).stringToJsonNode(json, source);
    }

    /**
     * Check if a file is a config file using the configuration file name.
     * This method returns {@code true} if the file match {@code (otp|build|router)-config.json}.
     */
    public static boolean isConfigFile(String filename) {
        return OTP_CONFIG_FILENAME.equals(filename)
                || BUILD_CONFIG_FILENAME.equals(filename)
                || ROUTER_CONFIG_FILENAME.equals(filename);
    }

    /**
     * Load the graph build configuration file as a JsonNode three. An empty node is
     * returned if the given {@code configDir}  is {@code null} or config file is NOT found.
     * <p>
     * This method also log all loaded parameters to the console.
     * <p>
     * @see #loadJsonFile for more details.
     */
    public OtpConfig loadOtpConfig() {
        return new OtpConfig(loadJsonByFilename(OTP_CONFIG_FILENAME), OTP_CONFIG_FILENAME, true);
    }

    /**
     * Load the graph build configuration file as a JsonNode three. An empty node is
     * returned if the given {@code configDir}  is {@code null} or config file is NOT found.
     * <p>
     * This method also log all loaded parameters to the console.
     * <p>
     * @see #loadJsonFile for more details.
     */
    public BuildConfig loadBuildConfig() {
        JsonNode node = loadJsonByFilename(BUILD_CONFIG_FILENAME);
        if(node.isMissingNode()) {
            return BuildConfig.DEFAULT;
        }
        return new BuildConfig(node, BUILD_CONFIG_FILENAME, true);
    }

    /**
     * Load the router configuration file as a JsonNode three. An empty node is
     * returned if the given {@code configDir}  is {@code null} or config file is NOT found.
     * <p>
     * @see #loadJsonFile for more details.
     */
    public RouterConfig loadRouterConfig() {
        JsonNode node = loadJsonByFilename(ROUTER_CONFIG_FILENAME);
        if(node.isMissingNode()) {
            return RouterConfig.DEFAULT;
        }
        return new RouterConfig(node, ROUTER_CONFIG_FILENAME, true);
    }

    /**
     * Load the router configuration file as a JsonNode three. An empty node is
     * returned if the given {@code configDir}  is {@code null} or config file is NOT found.
     * <p>
     * @see #loadJsonFile for more details.
     */
    private JsonNode loadJsonByFilename(String filename) {
        // Use default parameters if no configDir is available.
        if (configDir == null) {
            if(jsonFallback != null) {
                return stringToJsonNode(jsonFallback, filename);
            }
            LOG.warn(
                    "Config '{}' not loaded, using defaults. Config directory not set.",
                    BUILD_CONFIG_FILENAME
            );
            return MissingNode.getInstance();
        }

        return loadJsonFile(new File(configDir, filename));
    }

    /**
     * Open and parse the JSON file at the given path into a Jackson JSON tree. Comments and
     * unquoted keys are allowed. Returns an empty node if the file does not exist. Throws an
     * exception if the file contains syntax errors or cannot be parsed for some other reason.
     * <p>
     * We do not require any JSON config files to be present because that would get in the way of
     * the simplest rapid deployment workflow. Therefore we return an empty JSON node when the file
     * is missing, causing us to fall back on all the default values as if there was a JSON file
     * present with no fields defined.
     */
    private JsonNode loadJsonFile(File file) {
        try {
            String configString = IOUtils.toString(
                    new FileInputStream(file), StandardCharsets.UTF_8
            );
            JsonNode node = stringToJsonNode(configString, file.toString());

            LOG.info("Load JSON configuration file '{}'", file.getPath());
            LOG.info("Summarizing '{}': {}", file.getPath(), node.toPrettyString());
            return node;
        }
        catch (FileNotFoundException ex) {
            LOG.info("File '{}' is not present. Using default configuration.", file);
            return MissingNode.getInstance();
        }
        catch (IOException e) {
            LOG.error("Error while parsing JSON config file '{}': {}", file, e.getMessage());
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Convert a String into JsonNode. Comments and unquoted fields are allowed in the given {@code
     * jsonAsString} input.
     *
     * @param source is used only to generate a human friendly error message in case of an error
     *               parsing the JSON or inserting environment variables.
     */
    private JsonNode stringToJsonNode(String jsonAsString, String source) {
        try {
            if(jsonAsString == null || jsonAsString.isBlank()) {
                return MissingNode.getInstance();
            }
            jsonAsString = insertEnvironmentVariables(jsonAsString, source);

            return mapper.readTree(jsonAsString);
        }
        catch (IOException ie) {
            LOG.error("Error while parsing config '{}'.", source, ie);
            throw new OtpAppException("Failed to load config: " + source);
        }
    }

    private void assertConfigDirIsADirectory() {
        // Config dir not set, using defaults
        if(configDir == null) {
            return;
        }
        if (!configDir.isDirectory()) {
            throw new IllegalArgumentException(
                    configDir + " is not a readable configuration directory."
            );
        }
    }
}
