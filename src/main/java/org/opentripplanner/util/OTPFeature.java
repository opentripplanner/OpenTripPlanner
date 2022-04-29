package org.opentripplanner.util;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this class is to be able to turn features on and off.
 * <p>
 * This configuration is optional and found under "feature" in the top level 'otp-config.json'
 * file.
 */
public enum OTPFeature {
  APIBikeRental(true),
  APIServerInfo(true),
  APIGraphInspectorTile(true),
  APIUpdaterStatus(true),
  MinimumTransferTimeIsDefinitive(false),
  OptimizeTransfers(true),
  ParallelRouting(false),
  TransferConstraints(true),
  FloatingBike(true),

  // Sandbox extension features - Must be turned OFF by default
  ActuatorAPI(false),
  DataOverlay(false),
  FlexRouting(false),
  GoogleCloudStorage(false),
  ReportApi(false),
  SandboxAPIGeocoder(false),
  SandboxAPILegacyGraphQLApi(false),
  SandboxAPIMapboxVectorTilesApi(false),
  SandboxAPITransmodelApi(false),
  SandboxAPIParkAndRideApi(false),
  TransferAnalyzer(false),
  VehicleToStopHeuristics(false);

  private static final Logger LOG = LoggerFactory.getLogger(OTPFeature.class);
  private boolean enabled;

  OTPFeature(boolean defaultEnabled) {
    this.enabled = defaultEnabled;
  }

  /**
   * This method allows the application to initialize each OTP feature. Only use this method at
   * startup-time.
   * <p>
   * THIS METHOD IS NOT THREAD-SAFE!
   */
  public static void enableFeatures(Map<OTPFeature, Boolean> map) {
    map.forEach(OTPFeature::set);
  }

  public static void logFeatureSetup() {
    LOG.info("Features turned on: \n\t" + valuesAsString(true));
    LOG.info("Features turned off: \n\t" + valuesAsString(false));
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
   * If feature is turned on, then return supplied object if not return {@code null}.
   */
  public <T> T isOnElseNull(Supplier<T> supplier) {
    return isOn() ? supplier.get() : null;
  }

  /* private members */

  /**
   * Allow unit test and this class to enable/disable a feature.
   */
  void set(boolean enabled) {
    this.enabled = enabled;
  }

  private static String valuesAsString(boolean enabled) {
    return Arrays
      .stream(values())
      .filter(it -> it.enabled == enabled)
      .map(Enum::name)
      .collect(Collectors.joining("\n\t"));
  }
}
