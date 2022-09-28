package org.opentripplanner.standalone.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an object representation of the 'otp-config.json'.
 */
public class OtpConfig {

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

  OtpConfig(JsonNode otpConfig, String source, boolean logUnusedParams) {
    this.root = new NodeAdapter(otpConfig, source);

    this.configVersion =
      root
        .of("configVersion")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asString(null);
    this.otpFeatures =
      root
        .of("otpFeatures")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asEnumMap(OTPFeature.class, Boolean.class);

    if (logUnusedParams && LOG.isWarnEnabled()) {
      root.logAllUnusedParameters(LOG::warn);
    }
  }
}
