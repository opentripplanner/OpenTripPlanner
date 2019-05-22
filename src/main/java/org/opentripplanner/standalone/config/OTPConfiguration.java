package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

/**
 * This class is responsible for loading OTP configuration. At the top level this is the
 * 'otp-config.json', and at the graph level it is the 'build-config.json' and the
 * 'router-config.json' files.
 * <p/>
 * Configuration is loaded lazy at startup form different configuration JSON files. Comments
 * and unquoted keys are allowed in the these configuration files. The configuration is cashed,
 * and not reloaded even if it is changed on the filesystem. This is a OTP design goal, the
 * priority is to make OTP restart fast, rather than supporting configuration reloading.
 * <p/>
 * We do not require any JSON config files to be present because that would get in the way
 * of the simplest rapid deployment workflow. Therefore we return an empty JSON node when
 * the file is missing, causing us to fall back on all the default values as if there was a
 * JSON file present with no fields defined.
 * <p/>
 * This class is responsible for logging when a configuration file is loaded, and if the
 * loading fails. It delegates most of this responsibility to the {@link ConfigFile}.
 * <p/>
 * It support multiple graph configurations to be loaded from respective directories; Hence
 * support for multiple Routers. (TODO OTP2 - Remove support for multiple routers)
 */
public class OTPConfiguration {
    private static final String OTP_CONFIG_FILENAME = "otp-config.json";

    /**
     * Make sure all configuration is loaded using the same parser, with the
     * same settings.
     */
    private final ObjectMapper parser;

    /**
     * The 'otp-config.json' file representation.
     */
    private final ConfigFile otpConfig;

    /**
     * Create a new OTP configuration instance for a given directory. OTP can load
     * multiple graphs with its own configuration.
     * TODO OTP2 - This feature will be removed i OTP2.
     */
    public OTPConfiguration(File otpRootPath) {
        this.otpConfig = new ConfigFile(otpRootPath, OTP_CONFIG_FILENAME);
        this.parser = createConfigParser();
    }

    /**
     * Retrieve config for a graph in the given directory. The configuration is
     * NOT cashed by this class.
     */
    public GraphConfig getGraphConfig(File graphPath) {
        return new GraphConfig(graphPath, parser);
    }

    /**
     * Return the global otp server configuration (otp-config.json). The
     * instance is cashed by this class.
     */
    public JsonNode otpConfig() {
        return otpConfig.load(parser);
    }


    /* private methods */

    /**
     * Create the configuration parser. Comments and unquoted keys are allowed.
     */
    private static ObjectMapper createConfigParser() {
        ObjectMapper configParser = new ObjectMapper();
        configParser.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        configParser.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        return configParser;
    }
}
