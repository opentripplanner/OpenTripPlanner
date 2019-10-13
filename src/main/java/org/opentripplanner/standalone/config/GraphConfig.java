package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

import java.io.File;

/**
 * Responsible for loading and cashing graph configuration - it loads both
 * the 'build-config.json' and the 'router-config.json' for a given
 * router.
 */
public class GraphConfig {
    private static final String BUILDER_CONFIG_FILENAME = "build-config.json";
    private static final String ROUTER_CONFIG_FILENAME = "router-config.json";

    private final File path;
    private final ObjectMapper parser;
    private final ConfigFile builderConfig;
    private final ConfigFile routerConfig;

    GraphConfig(File graphPath, ObjectMapper parser) {
        this.path = graphPath;
        this.parser = parser;
        this.builderConfig = new ConfigFile(graphPath, BUILDER_CONFIG_FILENAME);
        this.routerConfig = new ConfigFile(graphPath, ROUTER_CONFIG_FILENAME);
    }

    /**
     * The graph/router directory path where the graph and configuration files exist.
     */
    public File getPath() {
        return path;
    }

    /**
     * Get the builder config, load from {@link #BUILDER_CONFIG_FILENAME} config file, if not loaded.
     * <p>
     * Returns a {@link MissingNode} if the file does not exist.
     * The program(OTP) exit, if the file contains syntax errors or cannot be parsed.
     */
    public JsonNode builderConfig() {
        return builderConfig.load(parser);
    }

    /**
     * Get the router config, load from {@link #ROUTER_CONFIG_FILENAME} config file, if not loaded.
     * <p>
     * Returns a {@link MissingNode} if the file does not exist.
     * The program(OTP) will exit, if the file contains syntax errors or cannot be parsed.
     */
    public JsonNode routerConfig() {
        return routerConfig.load(parser);
    }

    /**
     * Same as the {@link #routerConfig()}, but uses the given
     * {@code embeddedRouterConfig} as a fallback if no configuration file is found.
     *
     * @param embeddedRouterConfig the configuration embedded as part of the graph serialization
     *                             and passed in here as a json string.
     */
    public JsonNode routerConfig(String embeddedRouterConfig) {
        return routerConfig.load(parser, embeddedRouterConfig);
    }

    /**
     * Test and return true if a filename matches one of the allowed configuration files.
     */
    public static boolean isGraphConfigFile(String filename) {
        return BUILDER_CONFIG_FILENAME.equals(filename) ||
                ROUTER_CONFIG_FILENAME.equals(filename);
    }
}
