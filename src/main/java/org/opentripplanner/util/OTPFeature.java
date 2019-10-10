package org.opentripplanner.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The purpose of this class is to be able to turn features on and off.
 * <p/>
 * This configuration is optional an found under "feature" in the top
 * level 'otp-config.json' file.
 */
public enum OTPFeature {
    APIExternalGeocoder(true),
    APIBikeRental(true),
    APIAlertPatcher(true),
    APIRouters(true),
    APIServerInfo(true),
    APIGraphInspectorTile(true),
    APIUpdaterStatus(true),

    // OTP Features

    // Sandbox extension features - Must be turned OFF by default
    SandboxExampleAPIGraphStatistics(false),
    TransferAnalyzer(false);

    private static final Logger LOG = LoggerFactory.getLogger(OTPFeature.class);

    OTPFeature(boolean defaultEnabled) {
        this.enabled = defaultEnabled;
    }

    private boolean enabled;

    /**
     * Return {@code true} if feature is turned 'on'.
     */
    public boolean isOn() {
        return enabled;
    }

    /**
     * Return {@code true} if feature is turned 'off'.
     */
    public boolean isOff() {
        return !enabled;
    }

    /**
     * Allow unit test and this class to enable/disable a feature.
     */
    void set(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Configure features using given JSON.
     */
    public static void configure(@NotNull JsonNode otpConfig) {
        JsonNode features = otpConfig.path("featuresEnabled");
        for (OTPFeature feature : values()) {
            setFeatureFromConfig(feature, features.path(feature.name()));
        }
        logFeatureSetup();
    }


    /* private members */

    private static void setFeatureFromConfig(OTPFeature feature, JsonNode node) {
        if (!node.isMissingNode()) {
            if(node.isBoolean()) {
                feature.set(node.booleanValue());
            }
            else {
                throw new IllegalArgumentException(
                        "Feature values is not boolean 'true' or 'false'." +
                        " Unable to parse value for feature '" + feature.name() + "'. Value: '" +
                        node.asText() + "'"
                );
            }
        }
    }

    private static void logFeatureSetup() {
        LOG.info("Features turned on: \n\t" + valuesAsString(true));
        LOG.info("Features turned off: \n\t" + valuesAsString(false));
    }

    private static String valuesAsString(boolean enabled) {
        return Arrays.stream(values())
                .filter(it -> it.enabled == enabled)
                .map(Enum::name)
                .collect(Collectors.joining("\n\t"));
    }
}
