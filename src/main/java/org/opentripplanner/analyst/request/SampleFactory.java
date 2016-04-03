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

package org.opentripplanner.analyst.request;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.core.SampleSource;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.OsmVertex;

import java.util.*;

public class SampleFactory implements SampleSource {

    public SampleFactory(Graph graph) {
        this.graph = graph;
        this.setSearchRadiusM(500);
    }

    private Graph graph;

    private double searchRadiusM;
    private double searchRadiusLat;

    /** When are two vertices considered equidistant and the origin should be moved slightly to avoid numerical issues? */
    private final double EPSILON = 1e-10;

    public void setSearchRadiusM(double radiusMeters) {
        this.searchRadiusM = radiusMeters;
        this.searchRadiusLat = SphericalDistanceLibrary.metersToDegrees(searchRadiusM);
    }

    @Override
    /** implements SampleSource interface */
    public Sample getSample(double lon, double lat) {
        Coordinate c = new Coordinate(lon, lat);
        // query always returns a (possibly empty) list, but never null
        Envelope env = new Envelope(c);
        // find scaling factor for equirectangular projection
        double xscale = Math.cos(c.y * Math.PI / 180);
        env.expandBy(searchRadiusLat / xscale, searchRadiusLat);
        @SuppressWarnings("unchecked")
        Collection<Vertex> vertices = graph.streetIndex.getVerticesForEnvelope(env);

        // make sure things are in the radius
        final TIntDoubleMap distances = new TIntDoubleHashMap();

        for (Vertex v : vertices) {
            if (!(v instanceof OsmVertex)) continue;

            // figure ersatz distance
            double dx = (lon - v.getLon()) * xscale;
            double dy = lat - v.getLat();
            distances.put(v.getIndex(), dx * dx + dy * dy);
        }

        
        List<Vertex> sorted = new ArrayList<Vertex>();
        
        for (Vertex input : vertices) {
            if (!(input instanceof OsmVertex &&
                    distances.get(input.getIndex()) < searchRadiusLat * searchRadiusLat))
                continue;

            for (StreetEdge e : Iterables.filter(input.getOutgoing(), StreetEdge.class)) {
                if (e.canTraverse(new TraverseModeSet(TraverseMode.WALK))) {
                    sorted.add(input);
                    break;
                }
            }
        }
        
        // sort list by distance
        Collections.sort(sorted, new Comparator<Vertex>() {

            @Override
            public int compare(Vertex o1, Vertex o2) {
                double d1 = distances.get(o1.getIndex());
                double d2 = distances.get(o2.getIndex());

                if (d1 < d2)
                    return -1;
                else if (d1 > d2)
                    return 1;
                else return 0;
            }
        });

        Vertex v0, v1;

        if (sorted.isEmpty())
            return null;
        else if (sorted.size() <= 2) {
            v0 = sorted.get(0);
            v1 = sorted.size() > 1 ? sorted.get(1) : null;

        }
        else {
            int vxi = 0;

            // Group them by distance
            Vertex[] vx = new Vertex[2];

            ArrayList<Vertex> grouped = new ArrayList<>();

            // here's the idea: accumulate vertices by distance, waiting until we find a gap
            // of at least EPSILON. Once we've done that, break ties using labels (which are OSM IDs).
            for (int i = 0; i < sorted.size(); i++) {
                if (vxi >= 2) break;

                if (grouped.isEmpty()) {
                    grouped.add(sorted.get(i));
                    continue;
                }

                double dlast = distances.get(sorted.get(i - 1).getIndex());
                double dthis = distances.get(sorted.get(i).getIndex());
                if (dthis - dlast < EPSILON) {
                    grouped.add(sorted.get(i));
                    continue;
                }
                else {
                    // we have a distinct group of vertices
                    // sort them by OSM IDs
                    // this seems like it would be slow but keep in mind that it will only do any work
                    // when there are multiple members of a group, which is relatively rare.
                    Collections.sort(grouped, (vv1, vv2) -> vv1.getLabel().compareTo(vv2.getLabel()));

                    // then loop over the list until it's empty or we've found two vertices
                    int gi = 0;
                    while (vxi < 2 && gi < grouped.size()) {
                        vx[vxi++] = grouped.get(gi++);
                    }

                    // get ready for the next group
                    grouped.clear();
                }
            }

            v0 = vx[0];
            v1 = vx[1];
        }

        double d0 = v0 != null ? SphericalDistanceLibrary.distance(v0.getLat(),  v0.getLon(), lat, lon) : 0;
        double d1 = v1 != null ? SphericalDistanceLibrary.distance(v1.getLat(),  v1.getLon(), lat, lon) : 0;
        return new Sample(v0, (int) d0, v1, (int) d1);
    }

    /**
     * DistanceToPoint.computeDistance() uses a LineSegment, which has a closestPoint method.
     * That finds the true distance every time rather than once the closest segment is known, 
     * and does not allow for equi-rectangular projection/scaling.
     * 
     * Here we want to compare squared distances to all line segments until we find the best one, 
     * then do the precise calculations.
     * 
     */
    public Sample findClosest(List<Edge> edges, Coordinate pt, double xscale) {
        Candidate c = new Candidate();
        // track the best geometry
        Candidate best = new Candidate();

        for (Edge edge : edges) {
            /* LineString.getCoordinates() uses PackedCoordinateSequence.toCoordinateArray() which
             * necessarily builds new Coordinate objects.CoordinateSequence.getOrdinate() reads them 
             * directly. */
            c.edge = edge;
            LineString ls = (LineString)(edge.getGeometry());

            // We used to require samples to link to OSM vertices, but that means that
            // walking to a transit stop adjacent to the sample requires walking to the
            // end of the street and back.

            // However, linking to splitter vertices means that this sample can only be egressed in one direction,
            // because the splitter vertex is only on one half of a bidirectional edge pair. Additionally, the splitter
            // vertices and the connected edges for the two directions are exactly coincident, which means that which
            // one of the two a sample gets linked to is effectively random.
            if (!edge.getFromVertex().getLabel().startsWith("osm:node:") || (edge instanceof StreetEdge && ((StreetEdge) edge).isBack()))
                continue;

            CoordinateSequence coordSeq = ls.getCoordinateSequence();
            int numCoords = coordSeq.size();
            for (int seg = 0; seg < numCoords - 1; seg++) {
                c.seg = seg;
                double x0 = coordSeq.getX(seg);
                double y0 = coordSeq.getY(seg);
                double x1 = coordSeq.getX(seg+1);
                double y1 = coordSeq.getY(seg+1);
                // use bounding rectangle to find a lower bound on (squared) distance ?
                // this would mean more squaring or roots.
                c.frac = GeometryUtils.segmentFraction(x0, y0, x1, y1, pt.x, pt.y, xscale);
                // project to get closest point
                // note: no need to multiply anything by xscale; the fraction is scaleless.
                c.x = x0 + c.frac * (x1 - x0);
                c.y = y0 + c.frac * (y1 - y0);
                // find ersatz distance to edge (do not take root)
                double dx = (c.x - pt.x) * xscale;
                double dy = c.y - pt.y;
                c.dist2 = dx * dx + dy * dy;
                // replace best segments
                if (c.dist2 < best.dist2) {
                    best.setFrom(c);
                }
            } // end loop over segments
        } // end loop over linestrings

        // if at least one vertex was found make a sample
        if (best.edge != null) {
            Vertex v0 = best.edge.getFromVertex();
            //Vertex v1 = best.edge.getToVertex();
            Vertex v1 = v0;
            double d = best.distanceTo(pt);
            if (d > searchRadiusM)
                return null;
            double d0 = d + best.distanceAlong();
            //double d1 = d + best.distanceToEnd();
            double d1 = d0;
            Sample s = new Sample(v0, (int) d0, v1, (int) d1);
            //System.out.println(s.toString());
            return s;
        } 
        return null;
    }

    private static class Candidate {

        double dist2 = Double.POSITIVE_INFINITY;
        Edge edge = null;
        int seg = 0;
        double frac = 0;
        double x;
        double y;

        public void setFrom(Candidate other) {
            dist2 = other.dist2;
            edge = other.edge;
            seg = other.seg;
            frac = other.frac;
            x = other.x;
            y = other.y;
        }

        public double distanceTo(Coordinate c) {
            return SphericalDistanceLibrary.fastDistance(y, x, c.y, c.x);
        }

        public double distanceAlong() {
            CoordinateSequence cs = ( (LineString)(edge.getGeometry()) ).getCoordinateSequence();
            double dist = 0;
            double x0 = cs.getX(0);
            double y0 = cs.getY(0);
            for (int s = 1; s < seg; s++) { 
                double x1 = cs.getX(s);
                double y1 = cs.getY(s);
                dist += SphericalDistanceLibrary.fastDistance(y0, x0, y1, x1);
                x0 = x1;
                y0 = y1;
            }
            dist += SphericalDistanceLibrary.fastDistance(y0, x0, y, x); // dist along partial segment
            return dist;
        }

        public double distanceToEnd() {
            CoordinateSequence cs = ( (LineString)(edge.getGeometry()) ).getCoordinateSequence();
            int s = seg + 1;
            double x0 = cs.getX(s);
            double y0 = cs.getY(s);
            double dist = SphericalDistanceLibrary.fastDistance(y0, x0, y, x); // dist along partial segment
            int nc = cs.size();
            for (; s < nc; s++) { 
                double x1 = cs.getX(s);
                double y1 = cs.getY(s);
                dist += SphericalDistanceLibrary.fastDistance(y0, x0, y1, x1);
                x0 = x1;
                y0 = y1;
            }
            return dist;
        }
    }


}
