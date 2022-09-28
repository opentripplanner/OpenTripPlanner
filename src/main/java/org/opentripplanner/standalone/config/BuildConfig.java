package org.opentripplanner.standalone.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.common.geometry.CompactElevationProfile;
import org.opentripplanner.datastore.api.OtpDataStoreConfig;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayConfig;
import org.opentripplanner.ext.fares.FaresConfiguration;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.standalone.config.feed.DemExtractsConfig;
import org.opentripplanner.standalone.config.feed.NetexDefaultsConfig;
import org.opentripplanner.standalone.config.feed.OsmDefaultsConfig;
import org.opentripplanner.standalone.config.feed.OsmExtractsConfig;
import org.opentripplanner.standalone.config.feed.TransitFeedsConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.sandbox.DataOverlayConfigMapper;
import org.opentripplanner.util.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an object representation of the 'build-config.json'.
 * <p>
 * These are parameters that when changed, necessitate a Graph rebuild. They are distinct from the
 * RouterParameters which can be applied to a pre-built graph or on the fly at runtime. Eventually
 * both classes may be initialized from the same config file so make sure there is no overlap in the
 * JSON keys used.
 * <p>
 * These used to be command line parameters, but there were getting to be too many of them and
 * besides, we want to allow different graph build configuration for each Graph.
 * <p>
 * TODO maybe have only one giant config file and just annotate the parameters to indicate which
 * ones trigger a rebuild ...or just feed the same JSON tree to two different classes, one of which
 * is the build configuration and the other is the router configuration.
 */
public class BuildConfig implements OtpDataStoreConfig {

  private static final Logger LOG = LoggerFactory.getLogger(BuildConfig.class);

  public static final BuildConfig DEFAULT = new BuildConfig(
    MissingNode.getInstance(),
    "DEFAULT",
    false
  );

  private static final double DEFAULT_SUBWAY_ACCESS_TIME_MINUTES = 2.0;

  /**
   * Match all filenames that contains "gtfs". The pattern is NOT Case sensitive.
   */
  private static final String DEFAULT_GTFS_PATTERN = "(?i)gtfs";

  /**
   * Match all filenames that contain "netex". The pattern is NOT Case sensitive.
   */
  private static final String DEFAULT_NETEX_PATTERN = "(?i)netex";

  /**
   * Match all filenames that ends with suffix {@code .pbf}, {@code .osm} or {@code .osm.xml}. The
   * pattern is NOT Case sensitive.
   */
  private static final String DEFAULT_OSM_PATTERN = "(?i)(\\.pbf|\\.osm|\\.osm\\.xml)$";

  /**
   * Default: {@code (?i).tiff?$} - Match all filenames that ends with suffix {@code .tif} or {@code
   * .tiff}. The pattern is NOT Case sensitive.
   */
  private static final String DEFAULT_DEM_PATTERN = "(?i)\\.tiff?$";

  /**
   * The root adaptor kept for reference and (de)serialization.
   */
  private final NodeAdapter root;

  /**
   * The config-version is a parameter which each OTP deployment may set to be able to query the OTP
   * server and verify that it uses the correct version of the config. The version must be injected
   * into the config in the operation deployment pipeline. How this is done is up to the
   * deployment.
   * <p>
   * The config-version have no effect on OTP, and is provided as is on the API. There is no syntax
   * or format check on the version and it can be any string.
   * <p>
   * Be aware that OTP uses the config embedded in the loaded graph if no new config is provided.
   * <p>
   * This parameter is optional, and the default is {@code null}.
   */
  public final String configVersion;

  /**
   * Generates nice HTML report of Graph errors/warnings. They are stored in the same location as
   * the graph.
   */
  public final boolean dataImportReport;

  /**
   * If the number of issues is larger then {@code #maxDataImportIssuesPerFile}, then the files will
   * be split in multiple files. Since browsers have problems opening large HTML files.
   */
  public final int maxDataImportIssuesPerFile;

  /**
   * Include all transit input files (GTFS) from scanned directory.
   */
  public final boolean transit;

  /**
   * Minutes necessary to reach stops served by trips on routes of route_type=1 (subway) from the
   * street. Perhaps this should be a runtime router parameter rather than a graph build parameter.
   */
  public final double subwayAccessTime;

  /**
   * Include street input files (OSM/PBF).
   */
  public final boolean streets;

  /**
   * Embed the Router config in the graph, which allows it to be sent to a server fully configured
   * over the wire.
   */
  public final boolean embedRouterConfig;

  /**
   * Perform visibility calculations on OSM areas (these calculations can be time consuming).
   */
  public final boolean areaVisibility;

  /**
   * Link unconnected entries to public transport platforms.
   */
  public final boolean platformEntriesLinking;

  /**
   * Based on GTFS shape data, guess which OSM streets each bus runs on to improve stop linking.
   */
  public final boolean matchBusRoutesToStreets;

  /** If specified, download NED elevation tiles from the given AWS S3 bucket. */
  public final S3BucketConfig elevationBucket;

  /**
   * Unit conversion multiplier for elevation values. No conversion needed if the elevation values
   * are defined in meters in the source data. If, for example, decimetres are used in the source
   * data, this should be set to 0.1.
   */
  public final double elevationUnitMultiplier;

  /**
   * A specific fares service to use.
   */
  public final FareServiceFactory fareServiceFactory;

  /**
   * Patterns for matching NeTEx zip files or directories. If the filename contains the given
   * pattern it is considered a match. Any legal Java Regular expression is allowed.
   * <p>
   * This parameter is optional.
   * <p>
   * Default: {@code (?i)netex} - Match all filenames that contain "netex". The default pattern is
   * NOT case sensitive.
   */
  private final Pattern netexLocalFilePattern;

  /**
   * Patterns for matching GTFS zip-files or directories. If the filename contains the given
   * pattern it is considered a match. Any legal Java Regular expression is allowed.
   * <p>
   * This parameter is optional.
   * <p>
   * Default: {@code (?i)gtfs} - Match all filenames that contain "gtfs". The default pattern is
   * NOT case sensitive.
   */
  private final Pattern gtfsLocalFilePattern;

  /**
   * Pattern for matching Open Street Map input files. If the filename contains the given pattern
   * it is considered a match. Any legal Java Regular expression is allowed.
   * <p>
   * This parameter is optional.
   * <p>
   * Default: {@code (?i)(.pbf|.osm|.osm.xml)$} - Match all filenames that ends with suffix {@code
   * .pbf}, {@code .osm} or {@code .osm.xml}. The default pattern is NOT case sensitive.
   */
  private final Pattern osmLocalFilePattern;

  /**
   * Pattern for matching elevation DEM files. If the filename contains the given pattern it is
   * considered a match. Any legal Java Regular expression is allowed.
   * <p>
   * This parameter is optional.
   * <p>
   * Default: {@code (?i).tiff?$} - Match all filenames that ends with suffix {@code .tif} or
   * {@code .tiff}. The default pattern is NOT case sensitive.
   */
  private final Pattern demLocalFilePattern;

  /**
   * Local file system path to Google Cloud Platform service accounts credentials file. The
   * credentials is used to access GCS urls. When using GCS from outside of the bucket cluster you
   * need to provide a path the the service credentials. Environment variables in the path is
   * resolved.
   * <p>
   * Example: {@code "credentialsFile" : "${MY_GOC_SERVICE}"} or {@code "app-1-3983f9f66728.json" :
   * "~/"}
   * <p>
   * This is a path to a file on the local file system, not an URI.
   * <p>
   * This parameter is optional. Default is {@code null}.
   */
  private final String gsCredentials;

  /**
   * URI to the street graph object file for reading and writing. The file is created or overwritten
   * if OTP saves the graph to the file.
   * <p>
   * Example: {@code "streetGraph" : "file:///Users/kelvin/otp/streetGraph.obj" }
   * <p>
   * This parameter is optional. Default is {@code null}.
   */
  private final URI streetGraph;

  /**
   * URI to the graph object file for reading and writing. The file is created or overwritten if OTP
   * saves the graph to the file.
   * <p>
   * Example: {@code "graph" : "gs://my-bucket/otp/graph.obj" }
   * <p>
   * This parameter is optional. Default is {@code null}.
   */
  private final URI graph;

  /**
   * URI to the directory where the graph build report should be written to. The html report is
   * written into this directory. If the directory exist, any existing files are deleted. If it does
   * not exist, it is created.
   * <p>
   * Example: {@code "osm" : "file:///Users/kelvin/otp/buildReport" }
   * <p>
   * This parameter is optional. Default is {@code null} in which case the report is skipped.
   */
  private final URI buildReportDir;

  /**
   * A custom OSM namer to use.
   */
  public final CustomNamer customNamer;

  /**
   * When loading OSM data, the input is streamed 3 times - one phase for processing RELATIONS, one
   * for WAYS and last one for NODES. Instead of reading the data source 3 times it might be faster
   * to cache the entire osm file im memory. The trade off is of cause that OTP might use more
   * memory while loading osm data. You can use this parameter to choose what is best for your
   * deployment depending on your infrastructure. Set the parameter to {@code true} to cache the
   * data, and to {@code false} to read the stream from the source each time. The default value is
   * {@code false}.
   */
  public final boolean osmCacheDataInMem;
  /**
   * This field indicates the pruning threshold for islands without stops. Any such island under
   * this size will be pruned.
   */
  public final int pruningThresholdIslandWithoutStops;
  /**
   * This field indicates the pruning threshold for islands with stops. Any such island under this
   * size will be pruned.
   */
  public final int pruningThresholdIslandWithStops;
  /**
   * This field indicates whether walking should be allowed on OSM ways tagged with
   * "foot=discouraged".
   */
  public final boolean banDiscouragedWalking;
  /**
   * This field indicates whether bicycling should be allowed on OSM ways tagged with
   * "bicycle=discouraged".
   */
  public final boolean banDiscouragedBiking;
  /**
   * Transfers up to this duration with the default walk speed value will be pre-calculated and
   * included in the Graph.
   */
  public final double maxTransferDurationSeconds;
  /**
   * This will add extra edges when linking a stop to a platform, to prevent detours along the
   * platform edge.
   */
  public final Boolean extraEdgesStopPlatformLink;
  /**
   * Netex specific build parameters.
   */
  public final NetexDefaultsConfig netexDefaults;

  /**
   * OpenStreetMap specific build parameters.
   */
  public final OsmDefaultsConfig osmDefaults;

  public final List<RouteRequest> transferRequests;

  /**
   * Visibility calculations for an area will not be done if there are more nodes than this limit.
   */
  public final int maxAreaNodes;
  /**
   * Config for the DataOverlay Sandbox module
   */
  public final DataOverlayConfig dataOverlay;
  /**
   * This field is used for mapping routes geometry shapes. It determines max distance between shape
   * points and their stop sequence. If mapper can not find any stops within this radius it will
   * default to simple stop-to-stop geometry instead.
   */
  public final double maxStopToShapeSnapDistance;

  /**
   * Whether we should create car P+R stations from OSM data.
   */
  public boolean staticParkAndRide;
  /**
   * Whether we should create bike P+R stations from OSM data.
   */
  public boolean staticBikeParkAndRide;
  /**
   * Maximal distance between stops in meters that will connect consecutive trips that are made with
   * same vehicle
   */
  public int maxInterlineDistance;
  /**
   * The distance between elevation samples in meters. Defaults to 10m, the approximate resolution
   * of 1/3 arc-second NED data. This should not be smaller than the horizontal resolution of the
   * height data used.
   */
  public double distanceBetweenElevationSamples;
  /**
   * The maximum distance to propagate elevation to vertices which have no elevation. By default,
   * this is 2000 meters.
   */
  public double maxElevationPropagationMeters;
  /**
   * When set to true (it is by default), the elevation module will attempt to read this file in
   * order to reuse calculations of elevation data for various coordinate sequences instead of
   * recalculating them all over again.
   */
  public boolean readCachedElevations;
  /**
   * When set to true (it is false by default), the elevation module will create a file of a lookup
   * map of the LineStrings and the corresponding calculated elevation data for those coordinates.
   * Subsequent graph builds can reuse the data in this file to avoid recalculating all the
   * elevation data again.
   */
  public boolean writeCachedElevations;
  /**
   * When set to true (it is false by default), the elevation module will include the Ellipsoid to
   * Geiod difference in the calculations of every point along every StreetWithElevationEdge in the
   * graph.
   * <p>
   * NOTE: if this is set to true for graph building, make sure to not set the value of {@link
   * RoutingResource#geoidElevation} to true otherwise OTP will add this geoid value again to all of
   * the elevation values in the street edges.
   */
  public boolean includeEllipsoidToGeoidDifference;
  /**
   * Whether or not to multi-thread the elevation calculations in the elevation module. The default
   * is set to false. For unknown reasons that seem to depend on data and machine settings, it might
   * be faster to use a single processor. If multi-threading is activated, parallel streams will be
   * used to calculate the elevations.
   */
  public boolean multiThreadElevationCalculations;
  /**
   * Limit the import of transit services to the given START date. Inclusive. If set, any transit
   * service on a day BEFORE the given date is dropped and will not be part of the graph. Use an
   * absolute date or a period relative to the date the graph is build(BUILD_DAY).
   * <p>
   * Optional, defaults to "-P1Y" (BUILD_DAY minus 1 year). Use an empty string to make it
   * unbounded.
   * <p>
   * Examples:
   * <ul>
   *     <li>{@code "2019-11-24"} - 24. November 2019.</li>
   *     <li>{@code "-P3W"} - BUILD_DAY minus 3 weeks.</li>
   *     <li>{@code "-P1Y2M"} - BUILD_DAY minus 1 year and 2 months.</li>
   *     <li>{@code ""} - Unlimited, no upper bound.</li>
   * </ul>
   *
   * @see LocalDate#parse(CharSequence) for date format accepted.
   * @see Period#parse(CharSequence) for period format accepted.
   */
  public LocalDate transitServiceStart;
  /**
   * Limit the import of transit services to the given END date. Inclusive. If set, any transit
   * service on a day AFTER the given date is dropped and will not be part of the graph. Use an
   * absolute date or a period relative to the date the graph is build(BUILD_DAY).
   * <p>
   * Optional, defaults to "P3Y" (BUILD_DAY plus 3 years). Use an empty string to make it
   * unbounded.
   * <p>
   * Examples:
   * <ul>
   *     <li>{@code "2021-12-31"} - 31. December 2021.</li>
   *     <li>{@code "P24W"} - BUILD_DAY plus 24 weeks.</li>
   *     <li>{@code "P1Y6M5D"} - BUILD_DAY plus 1 year, 6 months and 5 days.</li>
   *     <li>{@code ""} - Unlimited, no lower bound.</li>
   * </ul>
   *
   * @see LocalDate#parse(CharSequence) for date format accepted.
   * @see Period#parse(CharSequence) for period format accepted.
   */
  public LocalDate transitServiceEnd;
  public final Set<String> boardingLocationTags;

  /**
   * Should minimum transfer times in GTFS files be discarded. This is useful eg. when the minimum
   * transfer time is only set for ticketing purposes, but we want to calculate the transfers always
   * from OSM data.
   */
  public boolean discardMinTransferTimes;

  /**
   * Time zone for the graph. This is used to store the timetables in the transit model, and to
   * interpret times in incoming requests.
   */
  public ZoneId transitModelTimeZone;

  /**
   * Whether to create stay-seated transfers in between two trips with the same block id.
   */
  public boolean blockBasedInterlining;

  /**
   * Specify parameters for DEM extracts. If not specified OTP will fall back to auto-detection
   * based on the directory provided on the command line.
   */
  public final DemExtractsConfig dem;

  /**
   * Specify parameters for OpensStreetMap extracts. If not specified OTP will fall back to
   * auto-detection based on the directory provided on the command line..
   */
  public final OsmExtractsConfig osm;

  /**
   * Specify parameters for transit feeds. If not specified OTP will fall back to auto-detection
   * based on the directory provided on the command line..
   */
  public final TransitFeedsConfig transitFeeds;

  /**
   * Set all parameters from the given Jackson JSON tree, applying defaults. Supplying
   * MissingNode.getInstance() will cause all the defaults to be applied. This could be done
   * automatically with the "reflective query scraper" but it's less type safe and less clear. Until
   * that class is more type safe, it seems simpler to just list out the parameters by name here.
   */
  public BuildConfig(JsonNode node, String source, boolean logUnusedParams) {
    this.root = new NodeAdapter(node, source);

    // Keep this list of BASIC parameters sorted alphabetically on config PARAMETER name
    areaVisibility = root.asBoolean("areaVisibility", false);
    banDiscouragedWalking = root.asBoolean("banDiscouragedWalking", false);
    banDiscouragedBiking = root.asBoolean("banDiscouragedBiking", false);
    configVersion =
      root.of("configVersion").doc(NA, "TODO DOC").example("2.2.12_12").asString(null);
    dataImportReport = root.asBoolean("dataImportReport", false);
    distanceBetweenElevationSamples =
      root.asDouble(
        "distanceBetweenElevationSamples",
        CompactElevationProfile.DEFAULT_DISTANCE_BETWEEN_SAMPLES_METERS
      );
    elevationBucket = S3BucketConfig.fromConfig(root.path("elevationBucket"));
    elevationUnitMultiplier = root.asDouble("elevationUnitMultiplier", 1);
    embedRouterConfig = root.asBoolean("embedRouterConfig", true);
    extraEdgesStopPlatformLink = root.asBoolean("extraEdgesStopPlatformLink", false);
    includeEllipsoidToGeoidDifference = root.asBoolean("includeEllipsoidToGeoidDifference", false);
    pruningThresholdIslandWithStops = root.asInt("islandWithStopsMaxSize", 5);
    pruningThresholdIslandWithoutStops = root.asInt("islandWithoutStopsMaxSize", 40);
    matchBusRoutesToStreets = root.asBoolean("matchBusRoutesToStreets", false);
    maxDataImportIssuesPerFile = root.asInt("maxDataImportIssuesPerFile", 1000);
    maxInterlineDistance = root.asInt("maxInterlineDistance", 200);
    blockBasedInterlining = root.asBoolean("blockBasedInterlining", true);
    maxTransferDurationSeconds =
      root.asDouble("maxTransferDurationSeconds", Duration.ofMinutes(30).toSeconds());
    maxStopToShapeSnapDistance = root.asDouble("maxStopToShapeSnapDistance", 150);
    multiThreadElevationCalculations = root.asBoolean("multiThreadElevationCalculations", false);
    osmCacheDataInMem = root.asBoolean("osmCacheDataInMem", false);
    platformEntriesLinking = root.asBoolean("platformEntriesLinking", false);
    readCachedElevations = root.asBoolean("readCachedElevations", true);
    staticBikeParkAndRide = root.asBoolean("staticBikeParkAndRide", false);
    staticParkAndRide = root.asBoolean("staticParkAndRide", true);
    streets = root.asBoolean("streets", true);
    subwayAccessTime = root.asDouble("subwayAccessTime", DEFAULT_SUBWAY_ACCESS_TIME_MINUTES);
    transit = root.asBoolean("transit", true);

    // Time Zone dependent config
    {
      // We need a time zone for setting transit service start and end. Getting the wrong time-zone
      // will just shift the period with one day, so the consequences is limited.
      transitModelTimeZone = root.asZoneId("transitModelTimeZone", null);
      var confZone = ObjectUtils.ifNotNull(transitModelTimeZone, ZoneId.systemDefault());
      transitServiceStart = root.asDateOrRelativePeriod("transitServiceStart", "-P1Y", confZone);
      transitServiceEnd = root.asDateOrRelativePeriod("transitServiceEnd", "P3Y", confZone);
    }

    writeCachedElevations = root.asBoolean("writeCachedElevations", false);
    maxAreaNodes = root.asInt("maxAreaNodes", 500);
    maxElevationPropagationMeters = root.asInt("maxElevationPropagationMeters", 2000);
    boardingLocationTags = root.asTextSet("boardingLocationTags", Set.of("ref"));
    discardMinTransferTimes = root.asBoolean("discardMinTransferTimes", false);

    var localFileNamePatternsConfig = root.path("localFileNamePatterns");
    gtfsLocalFilePattern = localFileNamePatternsConfig.asPattern("gtfs", DEFAULT_GTFS_PATTERN);
    netexLocalFilePattern = localFileNamePatternsConfig.asPattern("netex", DEFAULT_NETEX_PATTERN);
    osmLocalFilePattern = localFileNamePatternsConfig.asPattern("osm", DEFAULT_OSM_PATTERN);
    demLocalFilePattern = localFileNamePatternsConfig.asPattern("dem", DEFAULT_DEM_PATTERN);

    gsCredentials = root.asText("gsCredentials", null);
    graph = root.asUri("graph", null);
    streetGraph = root.asUri("streetGraph", null);
    buildReportDir = root.asUri("buildReportDir", null);

    osm = new OsmExtractsConfig(root.path("osm"));
    dem = new DemExtractsConfig((root.path("dem")));
    transitFeeds = new TransitFeedsConfig(root.path("transitFeeds"));

    // List of complex parameters
    fareServiceFactory = FaresConfiguration.fromConfig(root.rawNode("fares"));
    customNamer = CustomNamer.CustomNamerFactory.fromConfig(root.rawNode("osmNaming"));
    netexDefaults = new NetexDefaultsConfig(root.path("netexDefaults"));
    osmDefaults = new OsmDefaultsConfig(root.path("osmDefaults"));
    dataOverlay = DataOverlayConfigMapper.map(root.path("dataOverlay"));

    if (root.path("transferRequests").isNonEmptyArray()) {
      transferRequests =
        root
          .path("transferRequests")
          .asList()
          .stream()
          .map(RoutingRequestMapper::mapRoutingRequest)
          .toList();
    } else {
      transferRequests = List.of(new RouteRequest());
    }

    if (logUnusedParams && LOG.isWarnEnabled()) {
      root.logAllUnusedParameters(LOG::warn);
    }
  }

  @Override
  public URI reportDirectory() {
    return buildReportDir;
  }

  @Override
  public String gsCredentials() {
    return gsCredentials;
  }

  @Override
  public List<URI> osmFiles() {
    return osm.osmFiles();
  }

  @Override
  public List<URI> demFiles() {
    return dem.demFiles();
  }

  @Nonnull
  @Override
  public List<URI> gtfsFiles() {
    return transitFeeds.gtfsFiles();
  }

  @Nonnull
  @Override
  public List<URI> netexFiles() {
    return transitFeeds.netexFiles();
  }

  @Override
  public URI graph() {
    return graph;
  }

  @Override
  public URI streetGraph() {
    return streetGraph;
  }

  @Override
  public Pattern gtfsLocalFilePattern() {
    return gtfsLocalFilePattern;
  }

  @Override
  public Pattern netexLocalFilePattern() {
    return netexLocalFilePattern;
  }

  @Override
  public Pattern osmLocalFilePattern() {
    return osmLocalFilePattern;
  }

  @Override
  public Pattern demLocalFilePattern() {
    return demLocalFilePattern;
  }

  /**
   * If {@code true} the config is loaded from file, in not the DEFAULT config is used.
   */
  public boolean isDefault() {
    return root.isEmpty();
  }

  public String toJson() {
    return root.isEmpty() ? "" : root.toJson();
  }

  public ServiceDateInterval getTransitServicePeriod() {
    return new ServiceDateInterval(transitServiceStart, transitServiceEnd);
  }

  public int getSubwayAccessTimeSeconds() {
    // Convert access time in minutes to seconds
    return (int) (subwayAccessTime * 60.0);
  }

  public NodeAdapter asNodeAdapter() {
    return root;
  }
}
