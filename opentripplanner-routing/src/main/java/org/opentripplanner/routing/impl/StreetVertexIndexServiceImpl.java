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
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.vertextypes.Intersection;
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

    public static final double MAX_DISTANCE_FROM_STREET = 0.005;

    private static final double MAX_SNAP_TO_INTERSECTION_DISTANCE = 0.00005;

    public StreetVertexIndexServiceImpl() {
    }

    public StreetVertexIndexServiceImpl(Graph graph) {
        this.graph = graph;
    }

    @PostConstruct
    public void setup() {
        edgeTree = new STRtree();
        HashSet<Edge> edges = new HashSet<Edge>();
        for (Vertex v : graph.getVertices()) {
            if (v.getType() == Intersection.class) {
                for (Edge e: v.getOutgoing()) {
                    if (e instanceof Street){
                        edges.add(e);
                        Envelope env = new Envelope(v.getCoordinate(), e.getToVertex().getCoordinate());
                        edgeTree.insert(env, e);
                    }
                }
            }
        }
        edgeTree.build();
    }

    @SuppressWarnings("unchecked")
    public Vertex getClosestVertex(final Coordinate c) {
        /*
         * Assume c is on a street.
         *
         * Find the nearest two vertices such that (a) those vertices share an edge, and (b) that
         * edge is roughly nearby this point.
         *
         * This is technically O(n^2) in nearby points, but pretty often, the nearest two points
         * will do you. The case that you have to watch out for is:
         *
         *     |
         *     |
         * +-- p --+
         *     |
         *     |
         *
         * Here, two roads dead end near the middle of a long road; they do not share an edge, and p
         * is on the long road. P's closest two vertices are the ends of the dead ends.
         */

        Envelope envelope = new Envelope(c);
        List<Edge> nearby = new LinkedList<Edge>();
        int i = 0;
        double envelopeGrowthRate = 0.0018;
        GeometryFactory factory = new GeometryFactory();
        Point p = factory.createPoint(c);
        while (nearby.size() < 1 && i < 4) {
            ++i;
            envelope.expandBy(envelopeGrowthRate);
            envelopeGrowthRate *= 2;

            nearby = edgeTree.query(envelope);

            Edge bestStreet = null;
            double bestDistance = Double.MAX_VALUE;
            for (Edge e: nearby) {
                Geometry g = e.getGeometry();
                double distance = g.distance(p);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestStreet = e;
                }
            }
            if (bestDistance <= MAX_DISTANCE_FROM_STREET) {
                Geometry g = bestStreet.getGeometry();
                LocationIndexedLine l = new LocationIndexedLine(g);
                LinearLocation location = l.project(c);

                Coordinate start = bestStreet.getFromVertex().getCoordinate();
                Coordinate end = bestStreet.getToVertex().getCoordinate();
                Coordinate nearestPoint = location.getCoordinate(g);
                if (nearestPoint.distance(start) < MAX_SNAP_TO_INTERSECTION_DISTANCE) {
                    return bestStreet.getFromVertex();
                } else if (nearestPoint.distance(end) < MAX_SNAP_TO_INTERSECTION_DISTANCE) {
                    return bestStreet.getToVertex();
                }
                return StreetLocation.createStreetLocation(c.toString(), bestStreet, location);
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
