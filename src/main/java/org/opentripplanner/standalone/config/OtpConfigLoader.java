package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import javax.annotation.Nullable;
import org.opentripplanner.standalone.config.framework.file.ConfigFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic config file loader. This is used to load all configuration files.
 * <p>
 * This class also provides logging when a config file is loaded. We load and parse config files
 * early to reveal syntax errors without waiting for graph build.
 */
public class OtpConfigLoader {

  private static final Logger LOG = LoggerFactory.getLogger(OtpConfigLoader.class);

  private static final String OTP_CONFIG_FILENAME = "otp-config.json";
  private static final String BUILD_CONFIG_FILENAME = "build-config.json";
  private static final String ROUTER_CONFIG_FILENAME = "router-config.json";

  @Nullable
  private final File configDir;

  @Nullable
  private final String jsonFallback;

  private OtpConfigLoader(File configDir, String jsonFallback) {
    this.configDir = configDir;
    this.jsonFallback = jsonFallback;
  }

  /**
   * Create a config loader that load config from the given directory.
   */
  public OtpConfigLoader(File configDir) {
    this(configDir, null);
  }

  /**
   * Create a config loader that load config from the given input json document. Use it with {@link
   * #loadBuildConfig()} or one of the other load methods to return a configuration for the given
   * input json string.
   */
  public static OtpConfigLoader fromString(String json) {
    return new OtpConfigLoader(null, json);
  }

  /**
   * Check if a file is a config file using the configuration file name. This method returns {@code
   * true} if the file match {@code (otp|build|router)-config.json}.
   */
  public static boolean isConfigFile(String filename) {
    return (
      OTP_CONFIG_FILENAME.equals(filename) ||
      BUILD_CONFIG_FILENAME.equals(filename) ||
      ROUTER_CONFIG_FILENAME.equals(filename)
    );
  }

  /**
   * Log the config-version for each configuration file. The logging is only performed if the
   * config-version is set.
   */
  public static void logConfigVersion(
    String otpConfigVersion,
    String buildConfigVersion,
    String routerConfigVersion
  ) {
    logConfigVersion(otpConfigVersion, OTP_CONFIG_FILENAME);
    logConfigVersion(buildConfigVersion, BUILD_CONFIG_FILENAME);
    logConfigVersion(routerConfigVersion, ROUTER_CONFIG_FILENAME);
  }

  /**
   * Load the graph build configuration file as a JsonNode three. An empty node is returned if the
   * given {@code configDir}  is {@code null} or config file is NOT found.
   * <p>
   * This method also log all loaded parameters to the console.
   * <p>
   */
  public OtpConfig loadOtpConfig() {
    return new OtpConfig(loadFromFile(OTP_CONFIG_FILENAME), OTP_CONFIG_FILENAME, true);
  }

  /**
   * Load the graph build configuration file as a JsonNode three. An empty node is returned if the
   * given {@code configDir}  is {@code null} or config file is NOT found.
   * <p>
   * This method also log all loaded parameters to the console.
   * <p>
   */
  public BuildConfig loadBuildConfig() {
    JsonNode node = loadFromFile(BUILD_CONFIG_FILENAME);
    if (node.isMissingNode()) {
      return BuildConfig.DEFAULT;
    }
    return new BuildConfig(node, BUILD_CONFIG_FILENAME, true);
  }

  /**
   * Load the router configuration file as a JsonNode three. An empty node is returned if the given
   * {@code configDir}  is {@code null} or config file is NOT found.
   * <p>
   */
  public RouterConfig loadRouterConfig() {
    JsonNode node = loadFromFile(ROUTER_CONFIG_FILENAME);
    if (node.isMissingNode()) {
      return RouterConfig.DEFAULT;
    }
    return new RouterConfig(node, ROUTER_CONFIG_FILENAME, true);
  }

  private static void logConfigVersion(String configVersion, String filename) {
    if (configVersion != null) {
      LOG.info("{} config-version is {}.", filename, configVersion);
    }
  }

  private JsonNode loadFromFile(String filename) {
    return ConfigFileLoader
      .of()
      .withConfigDir(configDir)
      .withJsonFallback(jsonFallback)
      .loadFromFile(filename);
  }
}
