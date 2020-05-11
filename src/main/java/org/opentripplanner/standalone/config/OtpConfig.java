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


    OtpConfig(JsonNode otpConfig, String source, boolean logUnusedParams) {
        this.rawConfig = otpConfig;
        NodeAdapter adapter = new NodeAdapter(otpConfig, source);

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
