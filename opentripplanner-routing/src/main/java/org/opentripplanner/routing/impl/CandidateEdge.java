package org.opentripplanner.routing.impl;

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.vertextype.StreetVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.LineString;

public class CandidateEdge {
    private static final double PLATFORM_PREFERENCE = 2.0;

    private static final double SIDEWALK_PREFERENCE = 1.5;

    private static final double CAR_PREFERENCE = 100;

    public final StreetEdge edge;

    public final StreetVertex endwiseVertex;

    double score;

    public final Coordinate nearestPointOnEdge;

    public final double directionToEdge;

    public final double directionOfEdge;

    public final double directionDifference;

    public final double distance;

    public CandidateEdge(StreetEdge e, Coordinate p, double preference, TraverseModeSet mode) {
        int platform = 0;
        if (mode.contains(TraverseMode.TRAINISH)) {
            platform |= StreetEdge.CLASS_TRAIN_PLATFORM;
        }
        if (mode.contains(TraverseMode.BUSISH)) {
            platform |= StreetEdge.CLASS_OTHER_PLATFORM;
        }
        edge = e;
        LineString edgeGeom = edge.getGeometry();
        CoordinateSequence coordSeq = edgeGeom.getCoordinateSequence();
        int numCoords = coordSeq.size();
        int bestSeg = 0;
        double bestDist2 = Double.POSITIVE_INFINITY;
        double bestFrac = 0;
        nearestPointOnEdge = new Coordinate();
        double xscale = Math.cos(p.y * Math.PI / 180);
        for (int seg = 0; seg < numCoords - 1; seg++) {
            double x0 = coordSeq.getX(seg);
            double y0 = coordSeq.getY(seg);
            double x1 = coordSeq.getX(seg+1);
            double y1 = coordSeq.getY(seg+1);
            double frac = GeometryUtils.segmentFraction(x0, y0, x1, y1, p.x, p.y, xscale);
            // project to get closest point
            double x = x0 + frac * (x1 - x0);
            double y = y0 + frac * (y1 - y0);
            // find ersatz distance to edge (do not take root)
            double dx = (x - p.x) * xscale;
            double dy = y - p.y;
            double dist2 = dx * dx + dy * dy;
            // replace best segments
            if (dist2 < bestDist2) {
                nearestPointOnEdge.x = x;
                nearestPointOnEdge.y = y;
                bestFrac = frac;
                bestSeg = seg;
                bestDist2 = dist2;
            }
        } // end loop over segments

        distance = Math.sqrt(bestDist2);//distanceLibrary.distance(p, nearestPointOnEdge);

        if (bestSeg == 0 && Math.abs(bestFrac) < 0.000001)
            endwiseVertex = (StreetVertex) edge.getFromVertex();
        else if (bestSeg == numCoords - 2 && Math.abs(bestFrac - 1.0) < 0.000001)
            endwiseVertex = (StreetVertex) edge.getToVertex();
        else
            endwiseVertex = null;
        score = distance * SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_KM * 1000 / 360.0;
        score /= preference;
        if ((e.getStreetClass() & platform) != 0) {
            // this is kind of a hack, but there's not really a better way to do it
            score /= PLATFORM_PREFERENCE;
        }
        if (e.getName().contains("sidewalk")) {
            // this is kind of a hack, but there's not really a better way to do it
            score /= SIDEWALK_PREFERENCE;
        }
        if (e.getPermission().allows(StreetTraversalPermission.CAR)
                || (e.getStreetClass() & platform) != 0) {
            // we're subtracting here because no matter how close we are to a good non-car
            // non-platform edge, we really want to avoid it in case it's a Pedway or other
            // weird and unlikely starting location.
            score -= CAR_PREFERENCE;
        }
        // break ties by choosing shorter edges; this should cause split streets to be preferred
        score += edge.getLength() / 1000000;
        double xd = nearestPointOnEdge.x - p.x;
        double yd = nearestPointOnEdge.y - p.y;
        directionToEdge = Math.atan2(yd, xd);
        int edgeSegmentIndex = bestSeg;
        Coordinate c0 = coordSeq.getCoordinate(edgeSegmentIndex);
        Coordinate c1 = coordSeq.getCoordinate(edgeSegmentIndex + 1);
        xd = c1.x - c1.y;
        yd = c1.y - c0.y;
        directionOfEdge = Math.atan2(yd, xd);
        double absDiff = Math.abs(directionToEdge - directionOfEdge);
        directionDifference = Math.min(2 * Math.PI - absDiff, absDiff);
        if (Double.isNaN(directionToEdge) || Double.isNaN(directionOfEdge)
                || Double.isNaN(directionDifference)) {
            StreetVertexIndexServiceImpl._log.warn("direction to/of edge is NaN (0 length?): {}", edge);
        }
    }

    public boolean endwise() {
        return endwiseVertex != null;
    }

    public boolean parallel() {
        return directionDifference < Math.PI / 2;
    }

    public boolean perpendicular() {
        return !parallel();
    }

    public double getScore() {
        return score;
    }

    public String toString() {
        return "CE(" + edge + ", " + score + ")";
    }
}