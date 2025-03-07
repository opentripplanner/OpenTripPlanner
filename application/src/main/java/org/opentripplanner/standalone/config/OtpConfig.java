package org.opentripplanner.standalone.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;

import java.util.Map;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.application.OtpFileNames;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an object representation of the 'otp-config.json'.
 */
public class OtpConfig {

  /**
   * This description is shared for otp-config, build-config and router-config.
   */
  public static final String CONFIG_VERSION_DESCRIPTION =
    """
    The config-version is a parameter which each OTP deployment may set to be able to query the
    OTP server and verify that it uses the correct version of the config. The version should be
    injected into the config in the (continuous) deployment pipeline. How this is done, is up to
    the deployment.

    The config-version has no effect on OTP, and is provided as is on the API. There is no syntax
    or format check on the version and it can be any string.

    Be aware that OTP uses the config embedded in the loaded graph if no new config is provided.
    """;

  private static final Logger LOG = LoggerFactory.getLogger(OtpConfig.class);

  public final NodeAdapter root;

  public final Map<OTPFeature, Boolean> otpFeatures;

  /**
   * The config-version is a parameter which each OTP deployment may set to be able to query the OTP
   * server and verify that it uses the correct version of the config. The version must be injected
   * into the config in the operation deployment pipeline. How this is done is up to the
   * deployment.
   * <p>
   * The config-version have no effect on OTP, and is provided as is on the API. There is not syntax
   * or format check on the version and it can be any string.
   * <p>
   * This parameter is optional, and the default is {@code null}.
   */
  public final String configVersion;

  public OtpConfig(NodeAdapter nodeAdapter, boolean logUnusedParams) {
    this.root = nodeAdapter;

    this.configVersion = root
      .of("configVersion")
      .since(V2_1)
      .summary("Deployment version of the *" + OtpFileNames.OTP_CONFIG_FILENAME + "*.")
      .description(CONFIG_VERSION_DESCRIPTION)
      .asString(null);
    this.otpFeatures = root
      .of("otpFeatures")
      .since(V2_0)
      .summary("Turn features on/off.")
      .asEnumMap(OTPFeature.class, Boolean.class);

    if (logUnusedParams && LOG.isWarnEnabled()) {
      root.logAllWarnings(LOG::warn);
    }
  }

  /**
   * Checks if any unknown or invalid parameters were encountered while loading the configuration.
   */
  public boolean hasUnknownParameters() {
    return root.hasUnknownParameters();
  }
}
