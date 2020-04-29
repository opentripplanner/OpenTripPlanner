package org.opentripplanner.standalone.configure;

import com.fasterxml.jackson.databind.node.MissingNode;
import org.opentripplanner.datastore.OtpDataStoreConfig;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.ConfigLoader;
import org.opentripplanner.standalone.config.OtpConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for loading OTP configuration files:
 * <ol>
 *     <li>{@code otp-config.json}</li>
 *     <li>{@code build-config.json}</li>
 *     <li>{@code router-config.json}</li>
 * </ol>
 * All files are loaded from the same base directory.
 * <p>
 * Configuration is loaded lazy at startup form different configuration JSON files. Comments
 * and unquoted keys are allowed in the these configuration files. The configuration is cashed,
 * and not reloaded even if it is changed on the filesystem. Changing some parameters would
 * require a new graph build - witch is complicated. So, instead we try to keep the system
 * simple.
 * <p>
 * We do not require any JSON config files to be present because that would get in the way
 * of the simplest rapid deployment workflow. Therefore we return an empty JSON node when
 * the file is missing, causing us to fall back on all the default values as if there was a
 * JSON file present with no fields defined.
 * <p>
 * This class is responsible for logging when a configuration file is loaded, and if the
 * loading fails. It delegates most of this responsibility to the {@link ConfigLoader}.
 */
public class OTPConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(OTPConfiguration.class);
    /**
     * The command line parameters provide the directory for loading config files, and
     * in some cases may have parameters used to parse/override the file configs.
     */
    private final CommandLineParameters cli;
    private final OtpConfig otpConfig;

    /**
     * The build-config in NOT final because it can be set from the embedded
     * graph.obj build-config after the graph is loaded.
     */
    private BuildConfig buildConfig;

    /**
     * The router-config in NOT final because it can be set from the embedded
     * graph.obj router-config after the graph is loaded.
     */
    private RouterConfig routerConfig;

    private OTPConfiguration(CommandLineParameters cli, ConfigLoader configLoader) {
        this.cli = cli;
        this.otpConfig = configLoader.loadOtpConfig();
        this.buildConfig = configLoader.loadBuildConfig();
        this.routerConfig = configLoader.loadRouterConfig();
    }

    /**
     * Create a new OTP configuration instance for a given directory. OTP can load
     * multiple graphs with its own configuration.
     */
    public OTPConfiguration(CommandLineParameters cli) {
        this(cli, new ConfigLoader(cli.getBaseDirectory()));
    }

    /**
     * Create a new config for test using the given JSON config. The config is used to initiate
     * the OTP config, build config and the router config.
     */
    public static OTPConfiguration createForTest(String configJson) {
        return new OTPConfiguration(
                new CommandLineParameters(),
                ConfigLoader.fromString(configJson)
        );
    }

    public void updateConfigFromSerializedGraph(BuildConfig buildConfig, RouterConfig routerConfig) {
        if(this.buildConfig.isDefault()) {
            LOG.info("Using the graph embedded JSON build configuration.");
            this.buildConfig = buildConfig;
        }
        if(this.routerConfig.isDefault()) {
            LOG.info("Using the graph embedded JSON router configuration.");
            this.routerConfig = routerConfig;
        }
    }

    /**
     * TODO OTP2 - Remove this, and inject config using interfaces into the necessary
     *           - components, decompiling them from the OTP application parameters.
     * @return the command line parameters
     */
    public CommandLineParameters getCli() {
        return cli;
    }

    /**
     * Get the otp config as a type safe java bean.
     */
    public OtpConfig otpConfig() {
        return otpConfig;
    }

    /**
     * Get the graph build config as a java bean - type safe.
     */
    public BuildConfig buildConfig() {
        return buildConfig;
    }

    /**
     * Get the {@code router-config.json} as JsonNode.
     * <p>
     * Returns a {@link MissingNode} if base directory is {@code null} or the file does not exist.
     * @throws RuntimeException if the file contains syntax errors or cannot be parsed.
     */
    public RouterConfig routerConfig() {
        return routerConfig;
    }

    /**
     * Create plug in config to the data store.
     */
    public OtpDataStoreConfig createDataStoreConfig() {
        return new OtpDataStoreConfigAdapter(cli.getBaseDirectory(), buildConfig().storage);
    }
}
