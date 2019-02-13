package org.opentripplanner.routing.flex;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.module.map.StreetMatcher;
import org.opentripplanner.routing.edgetype.flex.FlexPatternHop;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TemporaryPartialStreetEdge;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * This class contains indices needed for flex service. Right now, that's a map between PatternHops
 * the StreetEdges which the hop is incident with. There may be more indices if new types of
 * flex transit are added.
 */
public class FlexIndex {

    private static final Logger LOG = LoggerFactory.getLogger(FlexIndex.class);

    private final Multimap<Edge, FlexPatternHop> hopsForEdge = HashMultimap.create();

    public void init(Graph graph) {
        LOG.info("initializing hops-for-edge map...");
        initializeHopsForEdgeMap(graph);
    }

    public Collection<FlexPatternHop> getHopsForEdge(Edge e) {
        if (e instanceof TemporaryPartialStreetEdge) {
            e = ((TemporaryPartialStreetEdge) e).getParentEdge();
        }
        return hopsForEdge.get(e);
    }

    private void initializeHopsForEdgeMap(Graph graph) {
        if (!graph.hasStreets) {
            LOG.info("Cannot initialize hop-to-street-edge map; graph does not have streets loaded.");
            return;
        }
        StreetMatcher matcher = new StreetMatcher(graph);
        LOG.info("Finding corresponding street edges for trip patterns...");
        for (TripPattern pattern : graph.index.patternForId.values()) {
            if (pattern.hasFlexService()) {
                LOG.debug("Matching {}", pattern);
                for(PatternHop ph : pattern.getPatternHops()) {
                    if (!ph.hasFlexService() || ! (ph instanceof FlexPatternHop)) {
                        continue;
                    }
                    FlexPatternHop patternHop = (FlexPatternHop) ph;
                    List<Edge> edges;
                    if (patternHop.getGeometry() == null) {
                        continue;
                    }
                    if (isSinglePoint(patternHop.getGeometry())) {
                        Coordinate pt = patternHop.getGeometry().getCoordinate();
                        edges = findClosestEdges(graph, pt);
                    }
                    else {
                        edges = matcher.match(patternHop.getGeometry());
                    }

                    if (edges == null || edges.isEmpty()) {
                        LOG.warn("Could not match to street network: {}", pattern);
                        continue;
                    }
                    for (Edge e : edges) {
                        hopsForEdge.put(e, patternHop);
                    }

                    // do the reverse, since we are walking and can go the other way.
                    edges = matcher.match(patternHop.getGeometry().reverse());
                    if (edges == null || edges.isEmpty()) {
                        continue;
                    }
                    for (Edge e : edges) {
                        hopsForEdge.put(e, patternHop);
                    }
                }
            }
        }
    }

    private List<Edge> findClosestEdges(Graph graph, Coordinate pointLocation) {
        if (graph.streetIndex == null)
            return Collections.emptyList();

        final double radiusDeg = SphericalDistanceLibrary.metersToDegrees(500);

        Envelope env = new Envelope(pointLocation);

        // local equirectangular projection
        double lat = pointLocation.getOrdinate(1);
        final double xscale = Math.cos(lat * Math.PI / 180);

        env.expandBy(radiusDeg / xscale, radiusDeg);

        Collection<Edge> edges = graph.streetIndex.getEdgesForEnvelope(env);
        if (edges.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Double, List<StreetEdge>> edgeDistanceMap = new TreeMap<>();
        for(Edge edge : edges){
            if(edge instanceof StreetEdge){
                LineString line = edge.getGeometry();
                double dist = SphericalDistanceLibrary.fastDistance(pointLocation, line);
                double roundOff = (double) Math.round(dist * 100) / 100;
                if(!edgeDistanceMap.containsKey(roundOff))
                    edgeDistanceMap.put(roundOff, new ArrayList<>());
                edgeDistanceMap.get(roundOff).add((StreetEdge) edge);
            }
        }

        List<Edge> closestEdges = edgeDistanceMap.values().iterator().next()
                .stream().map(e -> (Edge) e).collect(Collectors.toList());
        return closestEdges;
    }

    private boolean isSinglePoint(LineString line) {
        Coordinate coord = line.getCoordinate();
        for (int i = 0; i < line.getNumPoints(); i++) {
            if (!coord.equals(line.getCoordinateN(i))) {
                return false;
            }
        }
        return true;
    }
}
