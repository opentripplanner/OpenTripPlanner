package org.opentripplanner.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The purpose of this class is to be able to turn features on and off.
 * <p>
 * This configuration is optional an found under "feature" in the top
 * level 'otp-config.json' file.
 */
public enum OTPFeature {
    APIExternalGeocoder(true),
    APIBikeRental(true),
    APIAlertPatcher(true),
    APIServerInfo(true),
    APIGraphInspectorTile(true),
    APIUpdaterStatus(true),

    // Sandbox extension features - Must be turned OFF by default
    ActuatorAPI(false),
    GoogleCloudStorage(false),
    SandboxAPITransmodelApi(false),
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
    public static void configure(Function<OTPFeature, Boolean> enableFeature) {
        for (OTPFeature feature : values()) {
            Boolean value = enableFeature.apply(feature);
            if(value != null) {
                feature.set(value);
            }
        }
        logFeatureSetup();
    }


    /* private members */

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
