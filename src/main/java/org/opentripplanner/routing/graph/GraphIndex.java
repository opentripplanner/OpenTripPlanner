package org.opentripplanner.routing.graph;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multimap;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.LuceneIndex;
import org.opentripplanner.common.geometry.HashGrid;
import org.opentripplanner.profile.ProfileData;
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

    // TODO: consistently key on model object or id string
    public final Map<String, Vertex> vertexForId = Maps.newHashMap();
    public final Map<String, Agency> agencyForId = Maps.newHashMap();
    public final Map<AgencyAndId, Stop> stopForId = Maps.newHashMap();
    public final Map<AgencyAndId, Trip> tripForId = Maps.newHashMap();
    public final Map<AgencyAndId, Route> routeForId = Maps.newHashMap();
    public final Map<AgencyAndId, String> serviceForId = Maps.newHashMap();
    public final Map<String, TripPattern> patternForId = Maps.newHashMap();
    public final Map<Stop, TransitStop> stopVertexForStop = Maps.newHashMap();
    public final Map<Trip, TripPattern> patternForTrip = Maps.newHashMap();
    public final Multimap<Route, TripPattern> patternsForRoute = ArrayListMultimap.create();
    public final Multimap<Stop, TripPattern> patternsForStop = ArrayListMultimap.create();
    public final Multimap<String, Stop> stopsForParentStation = ArrayListMultimap.create();
    public final HashGrid<TransitStop> stopSpatialIndex = new HashGrid<TransitStop>();

    /* Full-text search extensions */
    public LuceneIndex lucene;

    /* Profile routing extensions */
    public ProfileData profile;

    public GraphIndex (Graph graph) {
        LOG.info("Indexing graph...");
        for (Agency a : graph.getAgencies()) {
            agencyForId.put(a.getId(), a);
        }
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
                stopsForParentStation.put(transitStop.getStop().getParentStation(), transitStop.getStop());
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
        analyzeServices();
        luceneIndex = new LuceneIndex(this, true);
        LOG.info("Done indexing graph.");
    }


    private void analyzeServices() {
        // This is a mess because CalendarService, CalendarServiceData, etc. are all in OBA.
        // TODO catalog days of the week and exceptions for each service day.
        // Make a table of which services are running on each calendar day.
    }

}
