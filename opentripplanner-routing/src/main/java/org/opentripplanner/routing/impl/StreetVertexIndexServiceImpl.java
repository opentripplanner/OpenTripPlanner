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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.StreetIntersectionVertex;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
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
import com.vividsolutions.jts.index.strtree.STRtree;
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

    private STRtree edgeTree;
    private STRtree transitStopTree;

    public static final double MAX_DISTANCE_FROM_STREET = 0.005;

    private static final double MAX_SNAP_DISTANCE = 0.00005;

    public StreetVertexIndexServiceImpl() {
    }

    public StreetVertexIndexServiceImpl(Graph graph) {
        this.graph = graph;
    }

    @PostConstruct
    public void setup() {
        edgeTree = new STRtree();
        transitStopTree = new STRtree();
        HashSet<Street> edges = new HashSet<Street>();
        for (Vertex v : graph.getVertices()) {
            if (v instanceof StreetIntersectionVertex) {
                for (Edge e: v.getOutgoing()) {
                    if (e instanceof Street){
                        edges.add((Street) e);
                        if (e.getGeometry() == null) {
                            continue;
                        }
                        Envelope env = new Envelope(v.getCoordinate(), e.getToVertex().getCoordinate());
                        edgeTree.insert(env, e);
                    }
                }
            } else if (v instanceof TransitStop) {
                Envelope env = new Envelope(v.getCoordinate());
                transitStopTree.insert(env, v);
            }
        }
        edgeTree.build();
        transitStopTree.build();
    }

    public Vertex getClosestVertex(final Coordinate c) {
        return getClosestVertex(c, true);
    }

    @SuppressWarnings("unchecked")
    public Vertex getClosestVertex(final Coordinate c, boolean includeTransitStops) {

        Envelope envelope = new Envelope(c);
        List<Street> nearby = new LinkedList<Street>();

        int i = 0;
        double envelopeGrowthRate = 0.0018;
        GeometryFactory factory = new GeometryFactory();
        Point p = factory.createPoint(c);
        while (nearby.size() < 1 && i < 4) {
            ++i;
            envelope.expandBy(envelopeGrowthRate);
            envelopeGrowthRate *= 2;

            // TODO: it kinda sucks to have multiple return statements ... maybe refactor to have a single return at end of method
            if (includeTransitStops) {
                double bestDistance = Double.MAX_VALUE;
                List<Vertex> nearbyTransitStops = transitStopTree.query(envelope);

                Vertex bestStop = null;
                for (Vertex v: nearbyTransitStops) {
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

            /* It is presumed, that edges which are the exact same distance from the examined
             * coordinate are parallel (coincident) edges. If this is wrong in too many cases,
             * than some finer logic will need to be added
             *
             * Parallel edges are needed to account for (oneway) streets with varying permissions.
             * i.e. using a C point on a oneway street a cyclist may go in one direction only, while
             * a pedestrian should be able to go in any direction. */

            double bestDistance = Double.MAX_VALUE;
            nearby = edgeTree.query(envelope);
            // FP - added condition and Double / null due to NPE problems
            if (nearby != null)
            {
                for (Street e: nearby) 
                {
                    if (e == null) continue;
                    Geometry g = e.getGeometry();
                    if(g != null) {
                        double distance = g.distance(p);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                        }
                    }
                }
                List<Street> parallel = new LinkedList<Street>();
                for (Street e: nearby) {
                    if (e == null) continue;
                    Geometry g = e.getGeometry();
                    if(g != null) {
                        double distance = g.distance(p);
                        if (distance == bestDistance) {
                            parallel.add(e);
                        }
                    }
                }
                if (bestDistance <= MAX_DISTANCE_FROM_STREET) {
                    Street bestStreet = parallel.get(0);
                    Geometry g = bestStreet.getGeometry();
                    LocationIndexedLine l = new LocationIndexedLine(g);
                    LinearLocation location = l.project(c);

                    Coordinate start = bestStreet.getFromVertex().getCoordinate();
                    Coordinate end = bestStreet.getToVertex().getCoordinate();
                    Coordinate nearestPoint = location.getCoordinate(g);
                    if (nearestPoint.distance(start) < MAX_SNAP_DISTANCE) {
                        return bestStreet.getFromVertex();
                    } else if (nearestPoint.distance(end) < MAX_SNAP_DISTANCE) {
                        return bestStreet.getToVertex();
                    }
                    return StreetLocation.createStreetLocation(bestStreet.getName() + "_" + c.toString(), bestStreet.getName(), parallel, location);
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
}
