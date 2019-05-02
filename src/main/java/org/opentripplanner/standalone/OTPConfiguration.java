package org.opentripplanner.standalone;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This class is responsible for loading OTP configuration. It make sure the configuration
 * is loaded lazy at startup. Comments and unquoted keys are allowed in the configuration
 * file.
 * <p/>
 * This class is also responsible for logging when a configuration file is loaded, and if the
 * loading fails.
 * <p/>
 * The configuration is cashed. An OTP design goal is to make OTP restart fast, rather than
 * supporting reloading configuration.
 * <p/>
 * It support multiple configurations to be loaded from respective base directories; Hence
 * support for multiple Routers. (TODO OTP2 - Remove support for multiple routers)
 * <p/>
 * We do not require any JSON config files to be present because that would get in the way
 * of the simplest rapid deployment workflow. Therefore we return an empty JSON node when
 * the file is missing, causing us to fall back on all the default values as if there was a
 * JSON file present with no fields defined.
 * <p/>
 * This class is thread safe when it comes to loading configuration files.
 */
public class OTPConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(OTPConfiguration.class);
    private static final String BUILDER_CONFIG_FILENAME = "build-config.json";
    private static final String ROUTER_CONFIG_FILENAME = "router-config.json";
    private static final String NO_FALLBACK_CONFIG_EXIST = null;

    private final ConfigFileInstance builderConfig;
    private final ConfigFileInstance routerConfig;
    private final ObjectMapper configParser;

    /**
     * Create a new OTP configuration instance for a given directory. OTP can load
     * multiple graphs with its own configuration.
     * TODO OTP2 - This feature will be removed i OTP2.
     */
    public OTPConfiguration(File configDir) {
        this.builderConfig = new ConfigFileInstance(configDir, BUILDER_CONFIG_FILENAME);
        this.routerConfig = new ConfigFileInstance(configDir, ROUTER_CONFIG_FILENAME);
        this.configParser = createConfigParser();
    }

    /**
     * Get the builder config, load from {@link #BUILDER_CONFIG_FILENAME} config file, if not loaded.
     * <p/>
     * Returns a {@link MissingNode} if the file does not exist.
     * The program(OTP) exit, if the file contains syntax errors or cannot be parsed.
     */
    public JsonNode builderConfig() {
        return builderConfig.loadIfNotLoaded(configParser, NO_FALLBACK_CONFIG_EXIST);
    }

    /**
     * Get the router config, load from {@link #ROUTER_CONFIG_FILENAME} config file, if not loaded.
     * <p/>
     * Returns a {@link MissingNode} if the file does not exist.
     * The program(OTP) will exit, if the file contains syntax errors or cannot be parsed.
     */
    public JsonNode routerConfig() {
        return routerConfig.loadIfNotLoaded(configParser, NO_FALLBACK_CONFIG_EXIST);
    }

    /**
     * Same as the {@link #routerConfig()}, but uses the given {@code embeddedRouterConfig} as a
     * fallback if no configuration file is found.
     *
     * @param embeddedRouterConfig the configuration embedded as part of the graph serialization
     *                             and passed in here as a json string.
     */
    public JsonNode routerConfig(String embeddedRouterConfig) {
        return routerConfig.loadIfNotLoaded(configParser, embeddedRouterConfig);
    }

    /**
     * Test and return true if a filename matches one of the allowed configuration files.
     */
    public static boolean isConfigFile(String filename) {
        return BUILDER_CONFIG_FILENAME.equals(filename) || ROUTER_CONFIG_FILENAME.equals(filename);
    }

    /**
     * Create the configuration parser. Comments and unquoted keys are allowed.
     */
    private static ObjectMapper createConfigParser() {
        ObjectMapper configParser = new ObjectMapper();
        configParser.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        configParser.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        return configParser;
    }

    /**
     * Load and cache a configuration file as a JsonNode. This class is also responsible
     * for logging: file loading and any errors.
     */
    private static class ConfigFileInstance {

        private final File configurationFile;

        /**
         * This flag is used to load the config file once. If it fails with an exception
         * the program continues without it, after logging an error. This flag make sure
         * that multiple request only trigger one loading of the configuration and that
         * the error is logged once.
         */
        private boolean loaded = false;

        /**
         * The cached config as a JSON tree.
         */
        private JsonNode config;

        private ConfigFileInstance(File configDir, String filename) {
            this.configurationFile = new File(configDir, filename);
        }

        /**
         * Return the loaded configuration. The configuration is loaded the first time this
         * method is called.
         * <p/>
         * Returns a {@link MissingNode} if the file does not exist.
         * <p/>
         * This method is synchronized to make sure the configuration is loaded once, and
         * that all other threads wait for it to complete loading.
         */
        JsonNode loadIfNotLoaded(ObjectMapper configParser, String fallbackConfig) {
            synchronized (this) {
                if (!loaded) {
                    loaded = true;
                    LOG.info("Load JSON configuration file '{}'", configurationFile);
                    this.config = loadJson(configurationFile, configParser);

                    if (config.isMissingNode()) {
                        //noinspection StringEquality, the constant is null
                        if(fallbackConfig != NO_FALLBACK_CONFIG_EXIST) {
                            LOG.info(
                                    "No {} configuration is found. Load embedded JSON configuration"
                                            + " from the graph object instead.",
                                    configurationFile.getName()
                            );
                            this.config = loadEmbeddedJson(fallbackConfig, configParser);
                        }
                        else {
                            LOG.info(
                                    "No {} configuration file is found. Using built-in OTP defaults.",
                                    configurationFile.getName()
                            );
                        }
                    }
                }
                return config;
            }
        }

        /**
         * Open and parse the JSON file at the given path into a Jackson JSON tree.
         * <p/>
         * Returns a {@link MissingNode} if the file does not exist.
         * The program(OTP) exit, if the file contains syntax errors or cannot be parsed for some
         * other reason.
         */
        private static JsonNode loadJson(File file, ObjectMapper mapper) {
            try (FileInputStream jsonStream = new FileInputStream(file)) {
                return mapper.readTree(jsonStream);
            } catch (FileNotFoundException ex) {
                return MissingNode.getInstance();
            } catch (Exception ex) {
                LOG.error("Error while parsing JSON config file '{}': {}", file, ex.getMessage());
                System.exit(42); // probably "should" be done with an exception
            }
            return null;
        }

        /**
         * Parse the given input JSON string into a Jackson JSON tree.
         * Throw a runtime exception on failure.
         */
        private static JsonNode loadEmbeddedJson(String jsonString, ObjectMapper mapper) {
            try {
                return mapper.readTree(jsonString);
            } catch (IOException e) {
                LOG.error("Can't read embedded config.");
                LOG.error(e.getMessage());
                throw new RuntimeException(e.getLocalizedMessage(), e);
            }
        }
    }
}
