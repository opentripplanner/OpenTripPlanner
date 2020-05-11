package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.common.geometry.CompactElevationProfile;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.Period;

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
public class BuildConfig {
    private static final Logger LOG = LoggerFactory.getLogger(BuildConfig.class);

    public static final BuildConfig DEFAULT = new BuildConfig(MissingNode.getInstance(), "DEFAULT", false);

    private static final double DEFAULT_SUBWAY_ACCESS_TIME_MINUTES = 2.0;

    /**
     * The raw JsonNode three kept for reference and (de)serialization.
     */
    private final JsonNode rawJson;

    /**
     * Generates nice HTML report of Graph errors/warnings. They are stored in the same location
     * as the graph.
     */
    public final boolean dataImportReport;

    /**
     * If the number of issues is larger then {@code #maxDataImportIssuesPerFile}, then the files
     * will be split in multiple files. Since browsers have problems opening large HTML files.
     */
    public final int maxDataImportIssuesPerFile;

    /**
     * Include all transit input files (GTFS) from scanned directory.
     */
    public final boolean transit;

    /**
     * Create direct transfer edges from transfers.txt in GTFS, instead of based on distance.
     */
    public final boolean useTransfersTxt;

    /**
     * Link GTFS stops to their parent stops.
     */
    public final boolean parentStopLinking;

    /**
     * Create direct transfers between the constituent stops of each parent station.
     */
    public final boolean stationTransfers;

    /**
     * Minutes necessary to reach stops served by trips on routes of route_type=1 (subway) from the street.
     * Perhaps this should be a runtime router parameter rather than a graph build parameter.
     */
    public final double subwayAccessTime;

    /**
     * Include street input files (OSM/PBF).
     */
    public final boolean streets;

    /**
     * Embed the Router config in the graph, which allows it to be sent to a server fully configured over the wire.
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

    /**
     * Download US NED elevation data and apply it to the graph.
     */
    public final boolean fetchElevationUS;

    /** If specified, download NED elevation tiles from the given AWS S3 bucket. */
    public final S3BucketConfig elevationBucket;

    /**
     * Unit conversion multiplier for elevation values. No conversion needed if the elevation values
     * are defined in meters in the source data. If, for example, decimetres are used in the source data,
     * this should be set to 0.1.
    */
    public final double elevationUnitMultiplier;

    /**
     * A specific fares service to use.
     */
    public final FareServiceFactory fareServiceFactory;

    /**
     * A custom OSM namer to use.
     */
    public final CustomNamer customNamer;

    /**
     * Custom OSM way properties
     */
    public final WayPropertySetSource wayPropertySet;

    /**
     * When loading OSM data, the input is streamed 3 times - one phase for processing RELATIONS,
     * one for WAYS and last one for NODES. Instead of reading the data source 3 times it might be
     * faster to cache the entire osm file im memory. The trade off is of cause that OTP might use
     * more memory while loading osm data. You can use this parameter to choose what is best for
     * your deployment depending on your infrastructure. Set the parameter to {@code true} to cache
     * the data, and to {@code false} to read the stream from the source each time. The default
     * value is {@code false}.
     */
    public final boolean osmCacheDataInMem;

    /**
     * Whether bike rental stations should be loaded from OSM, rather than periodically dynamically pulled from APIs.
     */
    public boolean staticBikeRental;

    /**
     * Whether we should create car P+R stations from OSM data.
     */
    public boolean staticParkAndRide;

    /**
     * Whether we should create bike P+R stations from OSM data.
     */
    public boolean staticBikeParkAndRide;

    /**
     * Maximal distance between stops in meters that will connect consecutive trips that are made with same vehicle
     */
    public int maxInterlineDistance;

    /**
     * This field indicates the pruning threshold for islands without stops.
     * Any such island under this size will be pruned.
     */
    public final int pruningThresholdIslandWithoutStops;

    /**
     * This field indicates the pruning threshold for islands with stops.
     * Any such island under this size will be pruned.
     */
    public final int pruningThresholdIslandWithStops;

    /**
     * This field indicates whether walking should be allowed on OSM ways
     * tagged with "foot=discouraged".
     */
    public final boolean banDiscouragedWalking;

    /**
     * This field indicates whether bicycling should be allowed on OSM ways
     * tagged with "bicycle=discouraged".
     */
    public final boolean banDiscouragedBiking;

    /**
     * Transfers up to this length in meters will be pre-calculated and included in the Graph.
     */
    public final double maxTransferDistance;

    /**
     * This will add extra edges when linking a stop to a platform, to prevent detours along the platform edge.
     */
    public final Boolean extraEdgesStopPlatformLink;

    /**
     * The distance between elevation samples in meters. Defaults to 10m, the approximate resolution of 1/3
     * arc-second NED data. This should not be smaller than the horizontal resolution of the height data used.
     */
    public double distanceBetweenElevationSamples;

    /**
     * When set to true (it is by default), the elevation module will attempt to read this file in order to reuse
     * calculations of elevation data for various coordinate sequences instead of recalculating them all over again.
     */
    public boolean readCachedElevations;

    /**
     * When set to true (it is false by default), the elevation module will create a file of a lookup map of the
     * LineStrings and the corresponding calculated elevation data for those coordinates. Subsequent graph builds can
     * reuse the data in this file to avoid recalculating all the elevation data again.
     */
    public boolean writeCachedElevations;

    /**
     * When set to true (it is false by default), the elevation module will include the Ellipsoid to Geiod difference in
     * the calculations of every point along every StreetWithElevationEdge in the graph.
     *
     * NOTE: if this is set to true for graph building, make sure to not set the value of
     * {@link RoutingResource#geoidElevation} to true otherwise OTP will add this geoid value again to all of the
     * elevation values in the street edges.
     */
    public boolean includeEllipsoidToGeoidDifference;

    /**
     * Whether or not to multi-thread the elevation calculations in the elevation module. The default is set to false.
     * For unknown reasons that seem to depend on data and machine settings, it might be faster to use a single
     * processor. If multi-threading is activated, parallel streams will be used to calculate the elevations.
     */
    public boolean multiThreadElevationCalculations;

    /**
     * Limit the import of transit services to the given START date. Inclusive. If set, any transit
     * service on a day BEFORE the given date is dropped and will not be part of the graph.
     * Use an absolute date or a period relative to the date the graph is build(BUILD_DAY).
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
     * @see LocalDate#parse(CharSequence) for date format accepted.
     * @see Period#parse(CharSequence) for period format accepted.
     */
    public LocalDate transitServiceStart;

    /**
     * Limit the import of transit services to the given END date. Inclusive. If set, any transit
     * service on a day AFTER the given date is dropped and will not be part of the graph.
     * Use an absolute date or a period relative to the date the graph is build(BUILD_DAY).
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
     * @see LocalDate#parse(CharSequence) for date format accepted.
     * @see Period#parse(CharSequence) for period format accepted.
     */
    public LocalDate transitServiceEnd;

    /**
     * Netex specific build parameters.
     */
    public final NetexConfig netex;

    /**
     * Otp auto detect input and output files using the command line supplied paths. This parameter
     * make it possible to override this by specifying a path for each file. All parameters in the
     * storage section is optional, and the fallback is to use the auto detection. It is OK to
     * autodetect some file and specify the path to others.
     */
    public final StorageConfig storage;

    /**
     * Set all parameters from the given Jackson JSON tree, applying defaults.
     * Supplying MissingNode.getInstance() will cause all the defaults to be applied.
     * This could be done automatically with the "reflective query scraper" but it's less type safe and less clear.
     * Until that class is more type safe, it seems simpler to just list out the parameters by name here.
     */
    public BuildConfig(JsonNode node, String source, boolean logUnusedParams) {
        NodeAdapter c = new NodeAdapter(node, source);
        rawJson = node;
        dataImportReport = c.asBoolean("dataImportReport", false);
        transit = c.asBoolean("transit", true);
        useTransfersTxt = c.asBoolean("useTransfersTxt", false);
        parentStopLinking = c.asBoolean("parentStopLinking", false);
        stationTransfers = c.asBoolean("stationTransfers", false);
        subwayAccessTime = c.asDouble("subwayAccessTime", DEFAULT_SUBWAY_ACCESS_TIME_MINUTES);
        streets = c.asBoolean("streets", true);
        embedRouterConfig = c.asBoolean("embedRouterConfig", true);
        areaVisibility = c.asBoolean("areaVisibility", false);
        platformEntriesLinking = c.asBoolean("platformEntriesLinking", false);
        matchBusRoutesToStreets = c.asBoolean("matchBusRoutesToStreets", false);
        fetchElevationUS = c.asBoolean("fetchElevationUS", false);
        elevationBucket = S3BucketConfig.fromConfig(c.path("elevationBucket"));
        elevationUnitMultiplier = c.asDouble("elevationUnitMultiplier", 1);
        fareServiceFactory = DefaultFareServiceFactory.fromConfig(c.asRawNode("fares"));
        customNamer = CustomNamer.CustomNamerFactory.fromConfig(c.asRawNode("osmNaming"));
        wayPropertySet = WayPropertySetSource.fromConfig(c.asText("osmWayPropertySet", "default"));
        osmCacheDataInMem = c.asBoolean("osmCacheDataInMem", false);
        staticBikeRental = c.asBoolean("staticBikeRental", false);
        staticParkAndRide = c.asBoolean("staticParkAndRide", true);
        staticBikeParkAndRide = c.asBoolean("staticBikeParkAndRide", false);
        maxDataImportIssuesPerFile = c.asInt("maxDataImportIssuesPerFile", 1000);
        maxInterlineDistance = c.asInt("maxInterlineDistance", 200);
        pruningThresholdIslandWithoutStops = c.asInt("islandWithoutStopsMaxSize", 40);
        pruningThresholdIslandWithStops = c.asInt("islandWithStopsMaxSize", 5);
        banDiscouragedWalking = c.asBoolean("banDiscouragedWalking", false);
        banDiscouragedBiking = c.asBoolean("banDiscouragedBiking", false);
        maxTransferDistance = c.asDouble("maxTransferDistance", 2000d);
        extraEdgesStopPlatformLink = c.asBoolean("extraEdgesStopPlatformLink", false);
        distanceBetweenElevationSamples = c.asDouble("distanceBetweenElevationSamples",
                CompactElevationProfile.DEFAULT_DISTANCE_BETWEEN_SAMPLES_METERS
        );
        readCachedElevations = c.asBoolean("readCachedElevations", true);
        writeCachedElevations = c.asBoolean("writeCachedElevations", false);
        includeEllipsoidToGeoidDifference = c.asBoolean("includeEllipsoidToGeoidDifference", false);
        multiThreadElevationCalculations = c.asBoolean("multiThreadElevationCalculations", false);
        transitServiceStart = c.asDateOrRelativePeriod("transitServiceStart", "-P1Y");
        transitServiceEnd = c.asDateOrRelativePeriod( "transitServiceEnd", "P3Y");

        netex = new NetexConfig(c.path("netex"));
        storage = new StorageConfig(c.path("storage"));

        if(logUnusedParams) {
            c.logAllUnusedParameters(LOG);
        }
    }

    /**
     * If {@code true} the config is loaded from file, in not the DEFAULT config is used.
     */
    public boolean isDefault() {
        return rawJson.isMissingNode();
    }

    public String toJson() {
        return rawJson.isMissingNode() ? "" : rawJson.toString();
    }

    public ServiceDateInterval getTransitServicePeriod() {
        return new ServiceDateInterval(
                new ServiceDate(transitServiceStart),
                new ServiceDate(transitServiceEnd)
        );
    }

    public int getSubwayAccessTimeSeconds() {
        // Convert access time in minutes to seconds
        return (int)(subwayAccessTime * 60.0);
    }
}
