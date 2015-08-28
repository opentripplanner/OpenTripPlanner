package org.opentripplanner.streets;

import com.vividsolutions.jts.geom.Envelope;
import gnu.trove.iterator.TIntIterator;
import org.apache.commons.math3.util.FastMath;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a potential split point along an existing edge, retaining some geometric calculation state so that
 * once the best candidate is found more detailed calculations can continue.
 * TODO handle initial and final Splits on same edge (use straight-line distance)
 */
public class Split {

    private static final Logger LOG = LoggerFactory.getLogger(Split.class);

    public int edge = -1;
    public int seg = 0; // the segment within the edge that is closest to the search point
    public double frac = 0; // the fraction along that segment where a link should occur
    public double fLon; // the x coordinate of the link point along the edge
    public double fLat; // the y coordinate of the link point along the edge
    // by virtue of having twice as many digits, a long could always hold the square of an int, but would it be faster than float math?
    public double distSquared = Double.POSITIVE_INFINITY;

    // The following fields require more calculations and are only set once a best edge is found.
    public int distance0_mm = 0; // the accumulated distance along the edge geometry up to the split point
    public int distance1_mm = 0; // the accumulated distance along the edge geometry after the split point
    public int distance_mm = 0; // the distance from the search point to the split point on the street
    public int vertex0; // the vertex at the beginning of the chosen edge
    public int vertex1; // the vertex at the end of the chosen edge

    public void setFrom (Split other) {
        edge = other.edge;
        seg = other.seg;
        frac = other.frac;
        fLon = other.fLon;
        fLat = other.fLat;
        distSquared = other.distSquared;
    }

    /**
     * @return a new Split object, or null if no edge was found in range.
     */
    public static Split find (double lat, double lon, double radiusMeters, StreetLayer streetLayer) {
        // NOTE THIS ENTIRE GEOMETRIC CALCULATION IS HAPPENING IN FIXED PRECISION INT DEGREES
        int fixLat = VertexStore.floatingDegreesToFixed(lat);
        int fixLon = VertexStore.floatingDegreesToFixed(lon);

        // We won't worry about the perpendicular walks yet.
        // Just insert or find a vertex on the nearest road and return that vertex.

        final double metersPerDegreeLat = 111111.111;
        double cosLat = FastMath.cos(FastMath.toRadians(lat)); // The projection factor, Earth is a "sphere"
        double radiusFixedLat = VertexStore.floatingDegreesToFixed(radiusMeters / metersPerDegreeLat);
        double radiusFixedLon = radiusFixedLat / cosLat; // Expand the X search space, don't shrink it.
        Envelope envelope = new Envelope(fixLon, fixLon, fixLat, fixLat);
        envelope.expandBy(radiusFixedLon, radiusFixedLat);
        double squaredRadiusFixedLat = radiusFixedLat * radiusFixedLat; // May overflow, don't use an int
        EdgeStore.Edge edge = streetLayer.edgeStore.getCursor();
        // Iterate over the set of forward (even) edges that may be near the given coordinate.
        TIntIterator edgeIterator = streetLayer.spatialIndex.query(envelope).iterator();
        // The split location currently being examined and the best one seen so far.
        Split curr = new Split();
        Split best = new Split();
        while (edgeIterator.hasNext()) {
            curr.edge = edgeIterator.next();
            edge.seek(curr.edge);
            edge.forEachSegment((seg, fLat0, fLon0, fLat1, fLon1) -> {
                // Find the fraction along the current segment
                curr.seg = seg;
                curr.frac = GeometryUtils.segmentFraction(fLon0, fLat0, fLon1, fLat1, fixLon, fixLat, cosLat);
                // Project to get the closest point on the segment.
                // Note: the fraction is scaleless, xScale is accounted for in the segmentFraction function.
                curr.fLon = fLon0 + curr.frac * (fLon1 - fLon0);
                curr.fLat = fLat0 + curr.frac * (fLat1 - fLat0);
                // Find squared distance to edge (avoid taking root)
                double dx = (curr.fLon - fixLon) * cosLat;
                double dy = (curr.fLat - fixLat);
                curr.distSquared = dx * dx + dy * dy;
                // Ignore segments that are too far away (filter false positives).
                // Replace the best segment if we've found something closer.
                if (curr.distSquared < squaredRadiusFixedLat && curr.distSquared < best.distSquared) {
                    best.setFrom(curr);
                }
            }); // end loop over segments
        } // end loop over edges

        if (best.edge < 0) {
            // No edge found nearby.
            return null;
        }

        // We found an edge. Iterate over its segments again, accumulating distances along its geometry.
        // The distance calculations involve square roots so are deferred to happen here, only on the selected edge.
        // TODO accumulate before/after geoms. Split point can be passed over since it's not an intermediate.
        // The length is are stored in one-element array to dodge Java's "effectively final" BS.
        edge.seek(best.edge);
        best.vertex0 = edge.getFromVertex();
        best.vertex1 = edge.getToVertex();
        double[] lengthBefore_fixedDeg = new double[1];
        edge.forEachSegment((seg, fLat0, fLon0, fLat1, fLon1) -> {
            // Sum lengths only up to the split point.
            // lengthAfter should be total length minus lengthBefore, which ensures splits do not change total lengths.
            if (seg <= best.seg) {
                double dx = (fLon1 - fLon0) * cosLat;
                double dy = (fLat1 - fLat0);
                double length = FastMath.sqrt(dx * dx + dy * dy);
                if (seg == best.seg) {
                    length *= best.frac;
                }
                lengthBefore_fixedDeg[0] += length;
            }
        });
        // Convert the fixed-precision degree measurements into (milli)meters
        double lengthBefore_floatDeg = VertexStore.fixedDegreesToFloating((int)lengthBefore_fixedDeg[0]);
        best.distance0_mm = (int)(lengthBefore_floatDeg * metersPerDegreeLat * 1000);
        // FIXME perhaps we should be using the sphericalDistanceLibrary here, or the other way around.
        // The initial edge lengths are set using that library on OSM node coordinates, and they are slightly different.
        // We are using a single cosLat value at the linking point, instead of a different value at each segment.
        if (best.distance0_mm < 0) {
            best.distance0_mm = 0;
            LOG.error("Length of first street segment was not positive.");
        }
        if (best.distance0_mm > edge.getLengthMm()) {
            LOG.error("Length of first street segment was greater than the whole edge ({} > {}).",
                    best.distance0_mm, edge.getLengthMm());
            best.distance0_mm = edge.getLengthMm();
        }
        best.distance1_mm = edge.getLengthMm() - best.distance0_mm;
        return best;
    }
}