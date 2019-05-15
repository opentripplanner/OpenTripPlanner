package org.opentripplanner.standalone.config;

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
 * Generic config file representation. This is used to load, not only the graph
 * configuration files, but also the otp application config file.
 * <p/>
 * Load and cache a configuration file as a JsonNode. This class is also provide
 * logging when a config file is loaded.
 */
class ConfigFile {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigFile.class);

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

    ConfigFile(File configDir, String filename) {
        this.configurationFile = new File(configDir, filename);
    }

    /**
     * Return the loaded configuration. The configuration is loaded the first time this
     * method is called.
     * <p/>
     * Returns a {@link MissingNode} if the file does not exist.
     */
    JsonNode load(ObjectMapper configParser) {
        return load(configParser, null);
    }

    /**
     * Return the loaded configuration. The configuration is loaded the first time this
     * method is called.
     * <p/>
     * Returns a {@link MissingNode} if the file does not exist.
     *
     * @param configParser   The parser to use.
     * @param fallbackConfig A JSON string to use if not config file is found.
     */
    JsonNode load(ObjectMapper configParser, String fallbackConfig) {
        if (!loaded) {
            loaded = true;
            LOG.info("Load JSON configuration file '{}'", configurationFile);
            this.config = loadJson(configurationFile, configParser);

            if (config.isMissingNode()) {
                if(fallbackConfig != null) {
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

    /**
     * Open and parse the JSON file at the given path into a Jackson JSON tree.
     * <p/>
     * Returns a {@link MissingNode} if the file does not exist.
     * The program(OTP) exit, if the file contains syntax errors or cannot be parsed for some
     * other reason.
     */
    private JsonNode loadJson(File file, ObjectMapper mapper) {
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
    private JsonNode loadEmbeddedJson(String jsonString, ObjectMapper mapper) {
        try {
            return mapper.readTree(jsonString);
        } catch (IOException e) {
            LOG.error("Can't read embedded config.");
            LOG.error(e.getMessage());
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }
}
