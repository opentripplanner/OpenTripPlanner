package org.opentripplanner.util;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this class is to be able to turn features on and off.
 * <p>
 * This configuration is optional and found under "feature" in the top
 * level 'otp-config.json' file.
 */
public enum OTPFeature {
    APIBikeRental(true),
    APIServerInfo(true),
    APIGraphInspectorTile(true),
    APIUpdaterStatus(true),
    OptimizeTransfers(true),
    ParallelRouting(true),
    TransferConstraints(true),

    // Sandbox extension features - Must be turned OFF by default
    ActuatorAPI(false),
    FlexRouting(false),
    FloatingBike(false),
    GoogleCloudStorage(false),
    ReportApi(false),
    SandboxAPITransmodelApi(false),
    SandboxAPILegacyGraphQLApi(false),
    SandboxAPIMapboxVectorTilesApi(false),
    SandboxExampleAPIGraphStatistics(false),
    SandboxAPIParkAndRideApi(false),
    TransferAnalyzer(false),
    DataOverlay(false);

    private static final Logger LOG = LoggerFactory.getLogger(OTPFeature.class);

    OTPFeature(boolean defaultEnabled) {
        this.enabled = defaultEnabled;
    }

    private boolean enabled;


    /**
     * This method allows the application to initialize each OTP feature. Only use this
     * method at startup-time.
     *
     * THIS METHOD IS NOT THREAD-SAFE!
     */
    public static void enableFeatures(Map<OTPFeature, Boolean> map) {
        map.forEach(OTPFeature::set);
    }

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
     * If feature is turned on, then return supplied object if not return {@code null}.
     */
    public <T> T isOnElseNull(Supplier<T> supplier) {
        return isOn() ? supplier.get() : null;
    }


    /* private members */

    public static void logFeatureSetup() {
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
