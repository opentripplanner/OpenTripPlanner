package org.opentripplanner.routing.graph;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.geometry.HashGrid;
import org.opentripplanner.routing.edgetype.TablePatternEdge;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class contains all the transient indexes of graph elements -- those that are not
 * serialized with the graph. Caching these maps is essentially an optimization, but a big one.
 * The index is bootstrapped from the graph's list of edges.
 */
public class GraphIndex {

    private static final Logger LOG = LoggerFactory.getLogger(GraphIndex.class);

    public final Map<String, Vertex>  vertexForId = Maps.newHashMap();
    public final Map<String, Agency>  agencyForId = Maps.newHashMap();
    public final Map<AgencyAndId, Stop> stopForId = Maps.newHashMap();
    public final Map<AgencyAndId, Trip> tripForId = Maps.newHashMap();
    public final Map<AgencyAndId, Route> routeForId = Maps.newHashMap();
    public final Map<String, TripPattern> patternForId = Maps.newHashMap();
    public final Map<Stop, TransitStop>   stopVertexForStop = Maps.newHashMap();
    public final Map<Trip, TripPattern> patternForTrip = Maps.newHashMap();
    public final ListMultimap<Route, TripPattern> patternsForRoute = ArrayListMultimap.create();
    public final ListMultimap<Stop, TripPattern>  patternsForStop  = ArrayListMultimap.create();
    
    public final HashGrid<TransitStop> stopSpatialIndex = new HashGrid<TransitStop>();
        
    public GraphIndex (Graph graph) {
        LOG.info("Indexing graph...");
        Collection<Edge> edges = graph.getEdges();
        /* We will keep a separate set of all vertices in case some have the same label. 
         * Maybe we should just guarantee unique labels. */
        Set<Vertex> vertices = Sets.newHashSet();
        for (Edge edge : edges) {
            vertices.add(edge.getFromVertex());
            vertices.add(edge.getToVertex());
            if (edge instanceof TablePatternEdge) {
                TablePatternEdge patternEdge = (TablePatternEdge) edge;
                TripPattern pattern = patternEdge.getPattern();
                patternForId.put(pattern.getCode(), pattern);
            }
        }
        for (Vertex vertex : vertices) {
            vertexForId.put(vertex.getLabel(), vertex);
            if (vertex instanceof TransitStop) {
                TransitStop transitStop = (TransitStop) vertex; 
                stopForId.put(transitStop.getStop().getId(), transitStop.getStop());
                stopVertexForStop.put(transitStop.getStop(), transitStop);
            }
        }
        stopSpatialIndex.setProjectionMeridian(vertices.iterator().next().getCoordinate().x);
        for (TransitStop stopVertex : stopVertexForStop.values()) {
            stopSpatialIndex.put(stopVertex.getCoordinate(), stopVertex);
        }
        for (TripPattern pattern : patternForId.values()) {
            patternsForRoute.put(pattern.route, pattern);
            for (Trip trip : pattern.getTrips()) {
                patternForTrip.put(trip, pattern);
                tripForId.put(trip.getId(), trip);
            }
            for (Stop stop: pattern.getStops()) {
                patternsForStop.put(stop, pattern);
            }
        }
        for (Route route : patternsForRoute.asMap().keySet()) {
            routeForId.put(route.getId(), route);
        }
        LOG.info("Done indexing graph.");
    }  
    
}
