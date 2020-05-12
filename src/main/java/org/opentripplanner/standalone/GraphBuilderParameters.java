package org.opentripplanner.standalone;

import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.graph_builder.module.ned.ElevationModule;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.profile.StopClusterMode;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.services.FareServiceFactory;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.List;

/**
 * These are parameters that when changed, necessitate a Graph rebuild.
 * They are distinct from the RouterParameters which can be applied to a pre-built graph or on the fly at runtime.
 * Eventually both classes may be initialized from the same config file so make sure there is no overlap
 * in the JSON keys used.
 * <p>
 * These used to be command line parameters, but there were getting to be too many of them and besides, we want to
 * allow different graph builder configuration for each Graph.
 * <p>
 * TODO maybe have only one giant config file and just annotate the parameters to indicate which ones trigger a rebuild
 * ...or just feed the same JSON tree to two different classes, one of which is the build configuration and the other is the router configuration.
 */
public class GraphBuilderParameters {

    private static double DEFAULT_SUBWAY_ACCESS_TIME = 2.0; // minutes

    /**
     * Generates nice HTML report of Graph errors/warnings (annotations). They are stored in the same location as the graph.
     */
    public final boolean htmlAnnotations;

    /**
     * If number of annotations is larger then specified number annotations will be split in multiple files.
     * Since browsers have problems opening large HTML files.
     */
    public final int maxHtmlAnnotationsPerFile;

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
     * Stop clusters can be built in one of two ways, either by geographical proximity and name, or
     * according to a parent/child station topology, if it exists.
     * <ul>
     * <li>"parentStation" See {@link GraphIndex#clusterByParentStation()}</li>
     * <li>"proximity" See {@link GraphIndex#clusterByProximityAndName()}. This is the default value.</li>
     * </ul>
     */
    public final StopClusterMode stopClusterMode;

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
     * Whether bike rental stations should be loaded from OSM, rather than periodically dynamically pulled from APIs.
     */
    public boolean staticBikeRental = false;

    /**
     * Whether we should create car P+R stations from OSM data.
     */
    public boolean staticParkAndRide = true;

    /**
     * Whether we should create bike P+R stations from OSM data.
     */
    public boolean staticBikeParkAndRide = false;

    /**
     * Maximal distance between stops in meters that will connect consecutive trips that are made with same vehicle
     */
    public int maxInterlineDistance = 200;

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
     * Set all parameters from the given Jackson JSON tree, applying defaults.
     * Supplying MissingNode.getInstance() will cause all the defaults to be applied.
     * This could be done automatically with the "reflective query scraper" but it's less type safe and less clear.
     * Until that class is more type safe, it seems simpler to just list out the parameters by name here.
     */
    public GraphBuilderParameters(JsonNode config) {
        htmlAnnotations = config.path("htmlAnnotations").asBoolean(false);
        transit = config.path("transit").asBoolean(true);
        useTransfersTxt = config.path("useTransfersTxt").asBoolean(false);
        parentStopLinking = config.path("parentStopLinking").asBoolean(false);
        stationTransfers = config.path("stationTransfers").asBoolean(false);
        stopClusterMode = enumValueOf(config, "stopClusterMode", StopClusterMode.proximity);
        subwayAccessTime = config.path("subwayAccessTime").asDouble(DEFAULT_SUBWAY_ACCESS_TIME);
        streets = config.path("streets").asBoolean(true);
        embedRouterConfig = config.path("embedRouterConfig").asBoolean(true);
        areaVisibility = config.path("areaVisibility").asBoolean(false);
        platformEntriesLinking = config.path("platformEntriesLinking").asBoolean(false);
        matchBusRoutesToStreets = config.path("matchBusRoutesToStreets").asBoolean(false);
        fetchElevationUS = config.path("fetchElevationUS").asBoolean(false);
        elevationBucket = S3BucketConfig.fromConfig(config.path("elevationBucket"));
        elevationUnitMultiplier = config.path("elevationUnitMultiplier").asDouble(1);
        fareServiceFactory = DefaultFareServiceFactory.fromConfig(config.path("fares"));
        customNamer = CustomNamer.CustomNamerFactory.fromConfig(config.path("osmNaming"));
        wayPropertySet = WayPropertySetSource.fromConfig(config.path("osmWayPropertySet").asText("default"));
        staticBikeRental = config.path("staticBikeRental").asBoolean(false);
        staticParkAndRide = config.path("staticParkAndRide").asBoolean(true);
        staticBikeParkAndRide = config.path("staticBikeParkAndRide").asBoolean(false);
        maxHtmlAnnotationsPerFile = config.path("maxHtmlAnnotationsPerFile").asInt(1000);
        maxInterlineDistance = config.path("maxInterlineDistance").asInt(200);
        pruningThresholdIslandWithoutStops = config.path("islandWithoutStopsMaxSize").asInt(40);
        pruningThresholdIslandWithStops = config.path("islandWithStopsMaxSize").asInt(5);
        banDiscouragedWalking = config.path("banDiscouragedWalking").asBoolean(false);
        banDiscouragedBiking = config.path("banDiscouragedBiking").asBoolean(false);
        maxTransferDistance = config.path("maxTransferDistance").asDouble(2000);
        extraEdgesStopPlatformLink = config.path("extraEdgesStopPlatformLink").asBoolean(false);
        readCachedElevations = config.path("readCachedElevations").asBoolean(true);
        writeCachedElevations = config.path("writeCachedElevations").asBoolean(false);
        includeEllipsoidToGeoidDifference = config.path("includeEllipsoidToGeoidDifference").asBoolean(false);
        multiThreadElevationCalculations = config.path("multiThreadElevationCalculations").asBoolean(false);
    }


    @SuppressWarnings("unchecked")
    static <T extends Enum<T>> T enumValueOf(JsonNode config, String propertyName, T defaultValue) {
        String valueAsString = config.path(propertyName).asText(defaultValue.name());
        try {
            return Enum.valueOf((Class<T>) defaultValue.getClass(), valueAsString);
        }
        catch (IllegalArgumentException ignore) {
            List<? extends Enum> legalValues = Arrays.asList(defaultValue.getClass().getEnumConstants());
            throw new IllegalArgumentException("The graph build parameter " + propertyName
                    + " value '" + valueAsString + "' is not in legal. Expected one of " + legalValues + ".");
        }
    }
}
