package org.opentripplanner.standalone;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * These are parameters that when changed, necessitate a Graph rebuild.
 * They are distinct from the RouterParameters which can be applied to a pre-built graph or on the fly at runtime.
 * Eventually both classes may be initialized from the same config file so make sure there is no overlap
 * in the JSON keys used.
 *
 * These used to be command line parameters, but there were getting to be too many of them and besides, we want to
 * allow different graph builder configuration for each Graph.
 * <p>
 * TODO maybe have only one giant config file and just annotate the parameters to indicate which ones trigger a rebuild
 * ...or just feed the same JSON tree to two different classes, one of which is the build configuration and the other is the router configuration.
 */
public class GraphBuilderParameters {

    /**
     * Generates nice HTML report of Graph errors/warnings (annotations). They are stored in the same location as the graph.
     */
    public final boolean htmlAnnotations;

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
    public final boolean parentStationTransfers;

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
     * Based on GTFS shape data, guess which OSM streets each bus runs on to improve stop linking.
     */
    public final boolean matchBusRoutesToStreets;

    /**
     * Download and use elevation data for the graph.
     */
    public final boolean elevation;


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
        parentStationTransfers = config.path("parentStationTransfers").asBoolean(false);
        streets = config.path("streets").asBoolean(true);
        embedRouterConfig = config.path("embedRouterConfig").asBoolean(true);
        areaVisibility = config.path("areaVisibility").asBoolean(false);
        matchBusRoutesToStreets = config.path("matchBusRoutesToStreets").asBoolean(false);
        elevation = config.path("elevation").asBoolean(false);

    }

}
