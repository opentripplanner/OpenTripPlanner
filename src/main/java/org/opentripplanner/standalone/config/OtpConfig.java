package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.util.OTPFeature;


/**
 * Wraps the 'otp-config.json'
 */
public class OtpConfig {
    public final JsonNode rawConfig;


    OtpConfig(JsonNode otpConfig) {
        this.rawConfig = otpConfig;
    }

    /**
     * Check if a OTP feature is enabled.
     */
    public Boolean isFeatureEnabled(OTPFeature feature) {
        JsonNode node = rawConfig.path("featuresEnabled").path(feature.name());
        if(node.isMissingNode()) {
            return null;
        }
        if(node.isBoolean()) {
            return node.booleanValue();
        }
        throw new IllegalArgumentException(
                "Feature values is not boolean 'true' or 'false'." +
                        " Unable to parse value for feature '" + feature.name() + "'. Value: '" +
                        node.asText() + "'"
        );
    }
}
