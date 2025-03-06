package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.node.MissingNode;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.framework.application.OtpFileNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for keeping a single instance of the OTP configuration in memory.
 * OTP load the config from the {@code baseDirectory}, and then if not found the config is read
 * from the serialized graph.
 * <p>
 * OTP load the following configuration files, see {@link OtpFileNames}. All files are loaded from
 * the same base directory.
 * <p>
 * Comments and unquoted keys are allowed in the these configuration files. The configuration is
 * cached, and not reloaded even if it is changed on the filesystem. Changing some parameters would
 * require a new graph build - which is complicated. So, instead we try to keep the system simple,
 * not allowing reloading the config parameters.
 * <p>
 * We do not require any JSON config files to be present because that would get in the way of the
 * simplest rapid deployment workflow. Therefor we return an empty JSON node when the file is
 * missing, causing us to fall back on all the default values as if there was a JSON file present
 * with no fields defined.
 * <p>
 * This class is responsible for logging when a configuration file is loaded, and if the
 * loading fails. It delegates most of this responsibility to the {@link OtpConfigLoader}.
 */
public class ConfigModel {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigModel.class);

  /**
   * The OTP config holding feature config.
   */
  private final OtpConfig otpConfig;

  /**
   * The build-config in NOT final because it can be set from the embedded graph.obj build-config
   * after the graph is loaded.
   */
  private BuildConfig buildConfig;

  /**
   * The router-config in NOT final because it can be set from the embedded graph.obj router-config
   * after the graph is loaded.
   */
  private RouterConfig routerConfig;

  private final DebugUiConfig debugUiConfig;

  public ConfigModel(
    OtpConfig otpConfig,
    BuildConfig buildConfig,
    RouterConfig routerConfig,
    DebugUiConfig debugUiConfig
  ) {
    this.otpConfig = otpConfig;
    this.buildConfig = buildConfig;
    this.routerConfig = routerConfig;
    this.debugUiConfig = debugUiConfig;

    initializeOtpFeatures(otpConfig);
  }

  public ConfigModel(OtpConfigLoader loader) {
    this(
      loader.loadOtpConfig(),
      loader.loadBuildConfig(),
      loader.loadRouterConfig(),
      loader.loadDebugUiConfig()
    );
  }

  public void updateConfigFromSerializedGraph(BuildConfig buildConfig, RouterConfig routerConfig) {
    if (this.buildConfig.isDefault()) {
      LOG.info("Using the graph embedded JSON build configuration.");
      this.buildConfig = buildConfig;
    }
    if (this.routerConfig.isDefault()) {
      LOG.info("Using the graph embedded JSON router configuration.");
      this.routerConfig = routerConfig;
    }
    OtpConfigLoader.logConfigVersion(
      this.otpConfig.configVersion,
      this.buildConfig.configVersion,
      this.routerConfig.getConfigVersion()
    );
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
   *
   * @throws RuntimeException if the file contains syntax errors or cannot be parsed.
   */
  public RouterConfig routerConfig() {
    return routerConfig;
  }

  public DebugUiConfig debugUiConfig() {
    return debugUiConfig;
  }

  public static void initializeOtpFeatures(OtpConfig otpConfig) {
    OTPFeature.enableFeatures(otpConfig.otpFeatures);
    OTPFeature.logFeatureSetup();
  }

  /**
   * Checks if any unknown or invalid parameters were encountered while loading the configuration.
   * <p>
   * If so it throws an exception.
   */
  public void abortOnUnknownParameters() {
    if (
      (otpConfig.hasUnknownParameters() ||
        buildConfig.hasUnknownParameters() ||
        routerConfig.hasUnknownParameters() ||
        debugUiConfig.hasUnknownParameters())
    ) {
      throw new OtpAppException(
        "Configuration contains unknown parameters (see above for details). " +
        "Please fix your configuration or remove --abortOnUnknownConfig from your OTP CLI parameters."
      );
    }
  }
}
