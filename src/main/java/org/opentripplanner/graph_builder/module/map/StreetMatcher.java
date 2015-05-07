/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.common.pqueue.BinHeap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

/**
 * This Performs most of the work for the MapBuilder graph builder module.
 * It determines which sequence of graph edges a GTFS shape probably corresponds to.
 * Note that GTFS shapes are not in any way constrained to OSM edges or even roads.
 */
public class StreetMatcher {
    private static final Logger log = LoggerFactory.getLogger(StreetMatcher.class);
    private static final double DISTANCE_THRESHOLD = 0.0002;

    Graph graph;

    private STRtree index;

    STRtree createIndex() {
        STRtree edgeIndex = new STRtree();
        for (Vertex v : graph.getVertices()) {
            for (Edge e : v.getOutgoing()) {
                if (e instanceof StreetEdge) {
                    Envelope envelope;
                    Geometry geometry = e.getGeometry();
                    envelope = geometry.getEnvelopeInternal();
                    edgeIndex.insert(envelope, e);
                }
            }
        }
        log.debug("Created index");
        return edgeIndex;
    }

    public StreetMatcher(Graph graph) {
        this.graph = graph;
        index = createIndex();
        index.build();
    }

    @SuppressWarnings("unchecked")
    public List<Edge> match(Geometry routeGeometry) {
        
        routeGeometry = removeDuplicatePoints(routeGeometry);

        if (routeGeometry == null) 
            return null;
        
        routeGeometry = DouglasPeuckerSimplifier.simplify(routeGeometry, 0.00001);

        // initial state: start midway along a block.
        LocationIndexedLine indexedLine = new LocationIndexedLine(routeGeometry);

        LinearLocation startIndex = indexedLine.getStartIndex();

        Coordinate routeStartCoordinate = startIndex.getCoordinate(routeGeometry);
        Envelope envelope = new Envelope(routeStartCoordinate);
        double distanceThreshold = DISTANCE_THRESHOLD;
        envelope.expandBy(distanceThreshold);

        BinHeap<MatchState> states = new BinHeap<MatchState>();
        List<Edge> nearbyEdges = index.query(envelope);
        while (nearbyEdges.isEmpty()) {
            envelope.expandBy(distanceThreshold);
            distanceThreshold *= 2;
            nearbyEdges = index.query(envelope);
        }

        // compute initial states
        for (Edge initialEdge : nearbyEdges) {
            Geometry edgeGeometry = initialEdge.getGeometry();
            
            LocationIndexedLine indexedEdge = new LocationIndexedLine(edgeGeometry);
            LinearLocation initialLocation = indexedEdge.project(routeStartCoordinate);
            
            double error = MatchState.distance(initialLocation.getCoordinate(edgeGeometry), routeStartCoordinate);
            MidblockMatchState state = new MidblockMatchState(null, routeGeometry, initialEdge, startIndex, initialLocation, error, 0.01);
            states.insert(state, 0); //make sure all initial states are visited by inserting them at 0
        }

        // search for best-matching path
        int seen_count = 0, total = 0;
        HashSet<MatchState> seen = new HashSet<MatchState>();
        while (!states.empty()) {
            double k = states.peek_min_key();
            MatchState state = states.extract_min();
            if (++total % 50000 == 0) {
                log.debug("seen / total: " + seen_count + " / " + total);
            }
            if (seen.contains(state)) {
                ++seen_count;
                continue;
            } else {
                if (k != 0) {
                    //but do not mark states as closed if we start at them
                    seen.add(state);
                }
            }
            if (state instanceof EndMatchState) {
                return toEdgeList(state);
            }
            for (MatchState next : state.getNextStates()) {
                if (seen.contains(next)) {
                    continue;
                }
                states.insert(next, next.getTotalError() - next.getDistanceAlongRoute());
            }
        }
        return null;
    }

    private Geometry removeDuplicatePoints(Geometry routeGeometry) {
        List<Coordinate> coords = new ArrayList<Coordinate>();
        Coordinate last = null;
        for (Coordinate c : routeGeometry.getCoordinates()) {
            if (!c.equals(last)) {
                last = c;
                coords.add(c);
            }
        }
        if (coords.size() < 2) {
            return null;
        }
        Coordinate[] coordArray = new Coordinate[coords.size()];
        return routeGeometry.getFactory().createLineString(coords.toArray(coordArray));
    }

    private List<Edge> toEdgeList(MatchState next) {
        ArrayList<Edge> edges = new ArrayList<Edge>();
        Edge lastEdge = null;
        while (next != null) {
            Edge edge = next.getEdge();
            if (edge != lastEdge) {
                edges.add(edge);
                lastEdge = edge;
            }
            next = next.parent;
        }
        Collections.reverse(edges);
        return edges;
    }
}
