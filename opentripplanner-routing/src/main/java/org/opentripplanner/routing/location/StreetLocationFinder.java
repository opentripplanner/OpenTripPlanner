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

package org.opentripplanner.routing.location;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.vertextypes.Intersection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

/**
 * This creates a StreetLocation representing a location on a street that's not at an intersection,
 * based on input latitude and longitude. Instantiating this class is expensive, because it creates
 * a spatial index of all of the intersections in the graph.
 */
public class StreetLocationFinder {

    Graph graph;

    STRtree intersections;

    public StreetLocationFinder(Graph graph) {
        this.graph = graph;
        intersections = new STRtree();
        for (Vertex v : graph.getVertices()) {
            if (v.getType() == Intersection.class) {
                Envelope env = new Envelope(v.getCoordinate());
                intersections.insert(env, v);
            }
        }
        intersections.build();
    }

    @SuppressWarnings("unchecked")
    public StreetLocation findLocation(String name, final Coordinate c, boolean incoming) {
        /*
         * Assume c is on a street.
         * 
         * Find the nearest two vertices such that (a) those vertices share an edge, and (b) that
         * edge is roughly nearby this point.
         * 
         * This is technically O(n^2) in nearby points, but pretty often, the nearest two points
         * will do you. The case that you have to watch out for is:
         * 
         * | | +-- p --+ | | r
         * 
         * Here, two roads dead end near the middle of a long road; they do not share an edge, and p
         * is on the long road. P's closest two vertices are the ends of the dead ends.
         */

        Envelope envelope = new Envelope(c);
        List<Vertex> nearby = new LinkedList<Vertex>();
        int i = 0;
        double envelopeGrowthRate = 0.0018;
        while (nearby.size() < 2 && i < 10) {
            ++i;
            envelope.expandBy(envelopeGrowthRate);
            envelopeGrowthRate *= 2;

            nearby = intersections.query(envelope);

            Collections.sort(nearby, new Comparator<Vertex>() {
                public int compare(Vertex a, Vertex b) {
                    double distance = (a.distance(c) - b.distance(c));
                    return (int) (Math.abs(distance) / distance);
                }

            });

            for (Vertex a : nearby) {
                for (Vertex b : nearby) {
                    if (a == b) {
                        continue;
                    }

                    // search the edges for one where a connects to b
                    Edge street = getEdgeWithToVertex(a, b);

                    if (street != null) {
                        LineString g = (LineString) street.getGeometry();
                        GeometryFactory factory = new GeometryFactory();
                        if (g.distance(factory.createPoint(c)) < 0.00000001) {
                            LengthIndexedLine l = new LengthIndexedLine(g);
                            double location = l.indexOf(c);
                            return StreetLocation.createStreetLocation(name, street, location, incoming);
                        }
                    }
                }
            }
        }
        throw new IntersectionNotFoundException("No intersection found near " + c
                + " within envelope " + envelope);
    }

    private Edge getEdgeWithToVertex(Vertex a, Vertex targetToVertex) {
        for( Edge street : a.getOutgoing() ) {
            if (street.getToVertex() == targetToVertex)
                return street;
        }
        for( Edge street : a.getIncoming() ) {
            if (street.getToVertex() == targetToVertex)
                return street;
        }
        return null;
    }
    
}
