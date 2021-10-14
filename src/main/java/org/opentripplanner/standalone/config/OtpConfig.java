package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


/**
 * This class is an object representation of the 'otp-config.json'.
 */
public class OtpConfig {
    private static final Logger LOG = LoggerFactory.getLogger(OtpConfig.class);

    public final JsonNode rawConfig;

    public final Map<OTPFeature, Boolean> otpFeatures;

    /**
     * The config-version is a parameter witch each OTP deployment may set to be able to
     * query the OTP server and verify that it uses the correct version of the config. The
     * version must be injected into the config in the operation deployment pipeline. How this
     * is done is up to the deployment.
     * <p>
     * The config-version have no effect on OTP, and is provided as is on the API. There is
     * not syntax or format check on the version and it can be any string.
     * <p>
     * This parameter is optional, and the default is {@code null}.
     */
    public final String configVersion;

    OtpConfig(JsonNode otpConfig, String source, boolean logUnusedParams) {
        this.rawConfig = otpConfig;
        NodeAdapter adapter = new NodeAdapter(otpConfig, source);

        this.configVersion = adapter.asText("configVersion", null);
        this.otpFeatures = adapter.asEnumMap(
                "otpFeatures",
                OTPFeature.class,
                NodeAdapter::asBoolean
        );
        if(logUnusedParams) {
            adapter.logAllUnusedParameters(LOG);
        }
    }
}
