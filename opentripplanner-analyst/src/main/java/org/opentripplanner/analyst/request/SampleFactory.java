package org.opentripplanner.analyst.request;

import java.util.List;

import org.opentripplanner.analyst.core.GeometryIndex;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.core.SampleSource;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.algorithm.distance.PointPairDistance;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;

@Component
public class SampleFactory implements SampleSource {

    private static DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    @Autowired
    private GeometryIndex index;

    private double searchRadiusM;
    private double searchRadiusLat;    

    public SampleFactory() {
        this.setSearchRadiusM(100);
    }
    
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
        List<Edge> edges = (List<Edge>) index.queryPedestrian(env);
        // look for edges and make a sample
        return findClosest(edges, c, xscale);
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
                c.frac = segmentFraction(x0, y0, x1, y1, pt.x, pt.y, xscale);
                // project to get closest point 
                c.x = x0 + c.frac * (x1 - x0);
                c.y = y0 + c.frac * (y1 - y0);
                // find ersatz distance to edge (do not take root)
                double dx = c.x - pt.x; // * xscale;
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
            Vertex v1 = best.edge.getToVertex();
            double d = best.distanceTo(pt);
            if (d > searchRadiusM)
                return null;
            double d0 = d + best.distanceAlong();
            int t0 = (int) (d0 / 1.33);
            double d1 = d + best.distanceToEnd();
            int t1 = (int) (d1 / 1.33);
            Sample s = new Sample(v0, t0, v1, t1);
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
            return distanceLibrary.fastDistance(y, x, c.y, c.x);
        }
        
        public double distanceAlong() {
            CoordinateSequence cs = ( (LineString)(edge.getGeometry()) ).getCoordinateSequence();
            double dist = 0;
            double x0 = cs.getX(0);
            double y0 = cs.getY(0);
            for (int s = 1; s < seg; s++) { 
                double x1 = cs.getX(s);
                double y1 = cs.getY(s);
                dist += distanceLibrary.fastDistance(y0, x0, y1, x1);
                x0 = x1;
                y0 = y1;
            }
            dist += distanceLibrary.fastDistance(y0, x0, y, x); // dist along partial segment 
            return dist;
        }

        public double distanceToEnd() {
            CoordinateSequence cs = ( (LineString)(edge.getGeometry()) ).getCoordinateSequence();
            int s = seg + 1;
            double x0 = cs.getX(s);
            double y0 = cs.getY(s);
            double dist = distanceLibrary.fastDistance(y0, x0, y, x); // dist along partial segment
            int nc = cs.size();
            for (; s < nc; s++) { 
                double x1 = cs.getX(s);
                double y1 = cs.getY(s);
                dist += distanceLibrary.fastDistance(y0, x0, y1, x1);
                x0 = x1;
                y0 = y1;
            }
            return dist;
        }
}
    
    /**
     * Adapted from com.vividsolutions.jts.geom.LineSegment 
     * Combines segmentFraction and projectionFactor methods.
     */
    private static double segmentFraction(double x0, double y0, double x1, double y1, 
            double xp, double yp, double xscale) {
        // identity checks are unnecessary, results are identical without them
        // if (x0 == xp && y0 == yp) return 0.0; // point is at beginning of segment
        // if (x1 == xp && y1 == yp) return 1.0; // point is at end of segment
        // Otherwise, use comp.graphics.algorithms Frequently Asked Questions method
        double dx = x1 - x0;
        double dy = y1 - y0;
        double len2 = dx * dx + dy * dy;
        // this fixes a (reported) divide by zero bug in JTS when line segment has 0 length
        if (len2 == 0)
            return 0;
        double r = ( (xp - x0) * dx + (yp - y0) * dy ) / len2;
        if (r < 0.0)
            return 0.0;
        else if (r > 1.0)
            return 1.0;
        return r;
      }
    
}
