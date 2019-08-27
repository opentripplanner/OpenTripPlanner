package org.opentripplanner.graph_builder.module.map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.model.Route;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.extra_graph.EdgesForRoute;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Uses the shapes from GTFS to determine which streets buses drive on. This is used to improve the quality of
 * the stop-to-street linkage. It encourages the linker to link to streets where transit actually travels.
 *
 * GTFS provides a mapping from trips->shapes. This module provides a mapping from stops->trips and shapes->edges.
 * Then transitively we get a mapping from stop->edges.
 * The edges that "belong" to a stop are favored when linking that stop to the street network.
 */
public class BusRouteStreetMatcher implements GraphBuilderModule {
    private static final Logger log = LoggerFactory.getLogger(BusRouteStreetMatcher.class);

    public List<String> provides() {
        return Arrays.asList("edge matching");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets", "transit");
    }

    /*
       The "extra" parameter is a mechanism for passing arbitrary things between graph builder modules.
       Whether or not this is a good idea is open to debate, but that's what it is.
       An EdgesForRoute instance is generated by MapBuilder and StreetMatcher, then retrieved later by the
       NetworkLinkerLibrary later (actually in LinkRequests).
     */
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        //Mapbuilder needs transit index
        graph.index(false);

        StreetMatcher matcher = new StreetMatcher(graph);
        EdgesForRoute edgesForRoute = new EdgesForRoute();
        extra.put(EdgesForRoute.class, edgesForRoute);
        log.info("Finding corresponding street edges for trip patterns...");
        // Why do we need to iterate over the routes? Why not just patterns?
        for (Route route : graph.index.routeForId.values()) {
            for (TripPattern pattern : graph.index.patternsForRoute.get(route)) {
                if (pattern.mode == TraverseMode.BUS) {
                    /* we can only match geometry to streets on bus routes */
                    log.debug("Matching {}", pattern);
                    //If there are no shapes in GTFS pattern geometry is generated
                    //generated geometry is useless for street matching
                    //that is why pattern.geometry is null in that case
                    if (pattern.geometry == null) {
                        continue;
                    }
                    List<Edge> edges = matcher.match(pattern.geometry);
                    if (edges == null || edges.isEmpty()) {
                        log.warn("Could not match to street network: {}", pattern);
                        continue;
                    }
                    List<Coordinate> coordinates = new ArrayList<Coordinate>();
                    for (Edge e : edges) {
                        coordinates.addAll(Arrays.asList(e.getGeometry().getCoordinates()));
                        edgesForRoute.edgesForRoute.put(route, e);
                    }
                    Coordinate[] coordinateArray = new Coordinate[coordinates.size()];
                    LineString ls = GeometryUtils.getGeometryFactory().createLineString(coordinates.toArray(coordinateArray));
                    // Replace the pattern's geometry from GTFS with that of the equivalent OSM edges.
                    pattern.geometry = ls;
                }
            }
        }
    }

    @Override
    public void checkInputs() {
        //no file inputs
    }
}
