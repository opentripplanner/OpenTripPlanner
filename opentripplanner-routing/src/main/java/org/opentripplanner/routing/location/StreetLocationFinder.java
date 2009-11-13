package org.opentripplanner.routing.location;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

public class StreetLocationFinder {

    Graph graph;

    STRtree intersections;

    public StreetLocationFinder(Graph graph) {
        this.graph = graph;
        intersections = new STRtree();
        for (Vertex v : graph.getVertices()) {
            Envelope env = new Envelope(v.getCoordinate());
            intersections.insert(env, v);
        }
        intersections.build();
    }

    public StreetLocation findLocation(final Coordinate c, boolean incoming) {
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
                    Edge street = null;
                    for (int j = 0; j < a.outgoing.size(); ++j) {
                        street = a.outgoing.get(j);
                        if (street.tov == b) {
                            break;
                        }
                    }
                    if (street == null) {
                        for (int j = 0; j < a.incoming.size(); ++j) {
                            street = a.incoming.get(j);
                            if (street.tov == b) {
                                break;
                            }
                        }
                    }

                    if (street != null) {
                        LineString g = (LineString) street.payload.getGeometry();
                        GeometryFactory factory = new GeometryFactory();
                        if (g.distance(factory.createPoint(c)) < 0.00000001) {
                            LengthIndexedLine l = new LengthIndexedLine(g);
                            double location = l.indexOf(c);
                            return new StreetLocation(street, location, incoming);
                        }
                    }
                }
            }
        }
        throw new IntersectionNotFoundException("No intersection found near " + c
                + " within envelope " + envelope);
    }
}
