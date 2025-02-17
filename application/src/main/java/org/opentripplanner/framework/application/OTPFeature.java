package org.opentripplanner.framework.application;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this class is to be able to turn features on and off.
 * <p>
 * This configuration is optional and found under "feature" in the top level 'otp-config.json'
 * file.
 */
public enum OTPFeature {
  AlertMetrics(
    false,
    false,
    "Starts a background thread to continuously publish metrics about alerts. Needs to be enabled together with `ActuactorAPI`."
  ),
  APIBikeRental(true, false, "Enable the bike rental endpoint."),
  APIServerInfo(true, false, "Enable the server info endpoint."),
  APIUpdaterStatus(true, false, "Enable endpoint for graph updaters status."),
  IncludeEmptyRailStopsInTransfers(
    false,
    false,
    """
      Turning this on guarantees that Rail stops without scheduled departures still get included
      when generating transfers using `ConsiderPatternsForDirectTransfers`. It is common for stops
      to be assign at real-time for Rail. Turning this on will help to avoid dropping transfers which
      are needed, when the stop is in use later. Turning this on, if
      ConsiderPatternsForDirectTransfers is off has no effect.
      """
  ),
  ConsiderPatternsForDirectTransfers(
    true,
    false,
    "Enable limiting transfers so that there is only a single transfer to each pattern."
  ),
  DebugUi(
    true,
    false,
    """
      Enable the debug GraphQL client and web UI and located at the root of the web server as well as the debug map tiles it uses.
      Be aware that the map tiles are not a stable API and can change without notice.
      Use the [vector tiles feature if](sandbox/MapboxVectorTilesApi.md) you want a stable map tiles API.
      """
  ),
  ExtraTransferLegOnSameStop(
    false,
    false,
    "Should there be a transfer leg when transferring on the very same stop. Note that for in-seat/interlined transfers no transfer leg will be generated."
  ),
  FloatingBike(true, false, "Enable floating bike routing."),
  GtfsGraphQlApi(true, false, "Enable the [GTFS GraphQL API](apis/GTFS-GraphQL-API.md)."),
  /**
   * If this feature flag is switched on, then the minimum transfer time is not the minimum transfer
   * time, but the definitive transfer time. Use this to override what we think the transfer will
   * take according to OSM data, for example if you want to set a very low transfer time like 1
   * minute, when walking the distance take 1m30s.
   *
   * TODO Harmonize the JavaDoc with the user doc and delete JavaDoc
   */
  MinimumTransferTimeIsDefinitive(
    false,
    false,
    "If the minimum transfer time is a lower bound (default) or the definitive time for the " +
    "transfer. Set this to `true` if you want to set a transfer time lower than what OTP derives " +
    "from OSM data."
  ),

  OptimizeTransfers(
    true,
    false,
    "OTP will inspect all itineraries found and optimize where (which stops) the transfer will happen. Waiting time, priority and guaranteed transfers are taken into account."
  ),

  ParallelRouting(false, false, "Enable performing parts of the trip planning in parallel."),
  TransferConstraints(
    true,
    false,
    "Enforce transfers to happen according to the _transfers.txt_ (GTFS) and Interchanges (NeTEx). Turning this _off_ will increase the routing performance a little."
  ),
  TransmodelGraphQlApi(
    true,
    true,
    "Enable the [Transmodel (NeTEx) GraphQL API](apis/TransmodelApi.md)."
  ),

  /* Sandbox extension features - Must be turned OFF by default */

  ActuatorAPI(false, true, "Endpoint for actuators (service health status)."),
  AsyncGraphQLFetchers(
    false,
    false,
    "Whether the @async annotation in the GraphQL schema should lead to the fetch being executed asynchronously. This allows batch or alias queries to run in parallel at the cost of consuming extra threads."
  ),
  WaitForGraphUpdateInPollingUpdaters(
    true,
    false,
    "Make all polling updaters wait for graph updates to complete before finishing. " +
    "If this is not enabled, the updaters will finish after submitting the task to update the graph."
  ),
  Co2Emissions(false, true, "Enable the emissions sandbox module."),
  DataOverlay(
    false,
    true,
    "Enable usage of data overlay when calculating costs for the street network."
  ),
  FaresV2(false, true, "Enable import of GTFS-Fares v2 data."),
  FlexRouting(false, true, "Enable FLEX routing."),
  GoogleCloudStorage(false, true, "Enable Google Cloud Storage integration."),
  LegacyRestApi(false, true, "Enable legacy REST API. This API will be removed in the future."),
  MultiCriteriaGroupMaxFilter(
    false,
    false,
    "Keep the best itinerary with respect to each criteria used in the transit-routing search. " +
    "For example the itinerary with the lowest cost, fewest transfers, and each unique transit-group " +
    "(transit-group-priority) is kept, even if the max-limit is exceeded. This is turned off by default " +
    "for now, until this feature is well tested."
  ),
  RealtimeResolver(
    false,
    true,
    "When routing with ignoreRealtimeUpdates=true, add an extra step which populates results with real-time data"
  ),
  ReportApi(false, true, "Enable the report API."),
  RestAPIPassInDefaultConfigAsJson(
    false,
    false,
    "Enable a default RouteRequest to be passed in as JSON on the REST API - FOR DEBUGGING ONLY!"
  ),
  SandboxAPIGeocoder(false, true, "Enable the Geocoder API."),
  SandboxAPIMapboxVectorTilesApi(false, true, "Enable Mapbox vector tiles API."),
  SandboxAPIParkAndRideApi(false, true, "Enable park-and-ride endpoint."),
  Sorlandsbanen(
    false,
    true,
    "Include train Sørlandsbanen in results when searching in south of Norway. Only relevant in Norway."
  ),
  TransferAnalyzer(false, true, "Analyze transfers during graph build.");

  private static final Object TEST_LOCK = new Object();

  private static final Logger LOG = LoggerFactory.getLogger(OTPFeature.class);

  private final boolean enabledByDefault;
  private final boolean sandbox;

  private boolean enabled;
  private final String doc;

  OTPFeature(boolean defaultEnabled, boolean sandbox, String doc) {
    this.enabledByDefault = defaultEnabled;
    this.enabled = defaultEnabled;
    this.sandbox = sandbox;
    this.doc = doc;
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
    LOG.info("Features turned on: \n\t{}", valuesAsString(true));
    LOG.info("Features turned off: \n\t{}", valuesAsString(false));
  }

  /**
   * FOR TEST ONLY
   *
   * This method will run the given {@code task} with the feature turned ON. When the task complete
   * the feature is set back to its original value.
   * <p>
   * This method is synchronized on the feature. This way calls to this method or the
   * {@link #testOff(Runnable)} is prevented from running concurrent. It is safe to use these
   * methods in a unit-test, but IT IS NOT SAFE TO USE IT IN GENERAL, because the main code is NOT
   * synchronized.
   */
  public void testOn(Runnable task) {
    testEnabled(true, task);
  }

  /**
   * FOR TEST ONLY
   *
   * See {@link #testOn(Runnable)}
   */
  public void testOff(Runnable task) {
    testEnabled(false, task);
  }

  /**
   * Return {@code true} if feature is turned 'on'.
   */
  public boolean isOn() {
    return enabled;
  }

  /**
   * Return {@code true} is enabled by default.
   */
  public boolean isEnabledByDefault() {
    return enabledByDefault;
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
  @Nullable
  public <T> T isOnElseNull(Supplier<T> supplier) {
    return isOn() ? supplier.get() : null;
  }

  /** This feature enable a Sandbox feature */
  public boolean isSandbox() {
    return sandbox;
  }

  /** Return user documentation. */
  public String doc() {
    return doc;
  }

  /* private members */

  /**
   * Allow unit test and this class to enable/disable a feature.
   */
  private void set(boolean enabled) {
    this.enabled = enabled;
  }

  private void testEnabled(boolean enabled, Runnable task) {
    synchronized (TEST_LOCK) {
      boolean originalValue = this.enabled;
      try {
        set(enabled);
        task.run();
      } finally {
        set(originalValue);
      }
    }
  }

  private static String valuesAsString(boolean enabled) {
    return Arrays
      .stream(values())
      .filter(it -> it.enabled == enabled)
      .map(Enum::name)
      .collect(Collectors.joining("\n\t"));
  }
}
