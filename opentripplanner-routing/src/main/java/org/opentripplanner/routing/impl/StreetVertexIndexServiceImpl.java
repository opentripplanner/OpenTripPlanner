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

package org.opentripplanner.routing.impl;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.core.TransitStop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

/**
 * This creates a StreetLocation representing a location on a street that's not at an intersection,
 * based on input latitude and longitude. Instantiating this class is expensive, because it creates
 * a spatial index of all of the intersections in the graph.
 */
@Component
public class StreetVertexIndexServiceImpl implements StreetVertexIndexService {

    private Graph graph;

    private SpatialIndex edgeTree;

    private STRtree transitStopTree;

    public static final double MAX_DISTANCE_FROM_STREET = 0.005;

    private static final double MAX_SNAP_DISTANCE = 0.0005;

    private static final double DISTANCE_ERROR = 0.00005;

    public StreetVertexIndexServiceImpl() {
    }

    public StreetVertexIndexServiceImpl(Graph graph) {
        this.graph = graph;
    }

    public void setup_modifiable() {
        edgeTree = new Quadtree();
        postSetup();
    }

    @PostConstruct
    public void setup() {
        edgeTree = new STRtree();
        postSetup();
        ((STRtree) edgeTree).build();
    }

    private void postSetup() {
        transitStopTree = new STRtree();
        for (GraphVertex gv : graph.getVertices()) {
            Vertex v = gv.vertex;
            for (Edge e : gv.getOutgoing()) {
                if (e instanceof TurnEdge || e instanceof OutEdge) {
                    if (e.getGeometry() == null) {
                        continue;
                    }
                    Envelope env = e.getGeometry().getEnvelopeInternal();
                    edgeTree.insert(env, e);
                }
            }
            if (v instanceof TransitStop) {
                // only index transit stops that (a) are entrances, or (b) have no associated
                // entrances
                TransitStop ts = (TransitStop) v;
                if (!ts.isEntrance()) {
                    boolean hasEntrance = false;

                    for (Edge e : gv.getOutgoing()) {
                        if (e instanceof PathwayEdge) {
                            hasEntrance = true;
                            break;
                        }
                    }
                    if (hasEntrance) {
                        continue;
                    }
                }
                Envelope env = new Envelope(v.getCoordinate());
                transitStopTree.insert(env, v);
            }
        }
        transitStopTree.build();
    }

    public Vertex getClosestVertex(Graph graph, final Coordinate c, TraverseOptions options) {
        return getClosestVertex(graph, c, true, true, options);
    }

    public Vertex getClosestVertex(Graph graph, final Coordinate c, TraverseOptions options, boolean forceEdges) {
        return getClosestVertex(graph, c, !forceEdges, !forceEdges, options);
    }

    
    @SuppressWarnings("unchecked")
    public Vertex getClosestVertex(Graph graph, final Coordinate c, boolean includeTransitStops,
            boolean allowSnappingToIntersections, TraverseOptions options) {

        Envelope envelope = new Envelope(c);
        List<Edge> nearby = new LinkedList<Edge>();

        int i = 0;
        double envelopeGrowthRate = 0.0018;
        GeometryFactory factory = new GeometryFactory();
        Point p = factory.createPoint(c);
        while (nearby.size() < 1 && i < 10) {
            ++i;
            envelope.expandBy(envelopeGrowthRate);
            envelopeGrowthRate *= 2;

            if (includeTransitStops) {
                double bestDistance = Double.MAX_VALUE;
                List<Vertex> nearbyTransitStops = transitStopTree.query(envelope);

                Vertex bestStop = null;
                for (Vertex v : nearbyTransitStops) {
                    Coordinate sc = v.getCoordinate();
                    double distance = sc.distance(c);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestStop = v;
                    }
                }
                if (bestDistance <= MAX_SNAP_DISTANCE) {
                    return bestStop;
                }
            }

            /*
             * It is presumed, that edges which are the exact same distance from the examined
             * coordinate are parallel (coincident) edges. If this is wrong in too many cases, than
             * some finer logic will need to be added
             * 
             * Parallel edges are needed to account for (oneway) streets with varying permissions.
             * i.e. using a C point on a oneway street a cyclist may go in one direction only, while
             * a pedestrian should be able to go in any direction.
             */

            double bestDistance = Double.MAX_VALUE;
            nearby = edgeTree.query(envelope);
            if (nearby != null) {
                Edge bestStreet = null;
                for (Edge e : nearby) {
                    if (e == null)
                        continue;
                    Geometry g = e.getGeometry();
                    if (g != null) {
                        double distance = g.distance(p);
                        if (distance < bestDistance) {
                            bestStreet = e;
                            bestDistance = distance;
                        }
                    }
                }
                if (bestDistance <= MAX_DISTANCE_FROM_STREET) {
                    Geometry g = bestStreet.getGeometry();
                    LocationIndexedLine l = new LocationIndexedLine(g);
                    LinearLocation location = l.project(c);

                    Vertex fromv = bestStreet.getFromVertex();
                    Vertex tov = bestStreet.getToVertex();
                    Coordinate start = fromv.getCoordinate();
                    Coordinate end = tov.getCoordinate();
                    Coordinate nearestPoint = location.getCoordinate(g);
                    if (allowSnappingToIntersections) {
                        if (nearestPoint.distance(start) < MAX_SNAP_DISTANCE) {
                            return fromv;
                        } else if (nearestPoint.distance(end) < MAX_SNAP_DISTANCE) {
                            return tov;
                        }
                    }
                    List<Edge> parallel = new LinkedList<Edge>();
                    for (Edge e : nearby) {
                        /* only include edges that this user can actually use */
                        if (e == null
                                || (options != null && e instanceof StreetEdge && !((StreetEdge) e)
                                        .canTraverse(options))) {
                            continue;
                        }
                        Geometry eg = e.getGeometry();
                        if (eg != null) {
                            double distance = eg.distance(p);
                            if (distance <= bestDistance + DISTANCE_ERROR) {
                                parallel.add(e);
                            }
                        }
                    }
                    return StreetLocation.createStreetLocation(graph, bestStreet.getName() + "_"
                            + c.toString(), bestStreet.getName(), parallel, nearestPoint);
                }
            }
        }
        return null;
    }

    @Autowired
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public Graph getGraph() {
        return graph;
    }

    public void reified(StreetLocation vertex) {
        for (Edge e : graph.getIncoming(vertex)) {
            if ((e instanceof TurnEdge || e instanceof OutEdge) && e.getGeometry() != null)
                edgeTree.insert(e.getGeometry().getEnvelopeInternal(), e);
        }
        for (Edge e : graph.getOutgoing(vertex)) {
            if ((e instanceof TurnEdge || e instanceof OutEdge) && e.getGeometry() != null)
                edgeTree.insert(e.getGeometry().getEnvelopeInternal(), e);
        }
    }
}
