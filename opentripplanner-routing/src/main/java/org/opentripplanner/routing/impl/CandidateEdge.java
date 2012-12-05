package org.opentripplanner.routing.impl;

import lombok.Getter;

import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
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

    private int platform;
    
    private double preference; 
    
    private CoordinateSequence edgeCoords;
    
    /**
     * Number of coordinates on the edge.
     */
    private int numEdgeCoords;
    
    /**
     * Index of the closest segment along the edge.
     */
    private int nearestSegmentIndex;
    
    /**
     * Fractional distance along the closest segment.
     */
    private double nearestSegmentFraction;
    
    /**
     * The edge itself.
     */
    public final StreetEdge edge;

    public final StreetVertex endwiseVertex;

    /**
     * Score of the match. Lower is better.
     */
    @Getter
    protected double score;

    /**
     * The coordinate of the nearest point on the edge.
     */
    public final Coordinate nearestPointOnEdge;
        
    public final double directionToEdge;

    public final double directionOfEdge;

    public final double directionDifference;

    public final double distance;
    
    public CandidateEdge(StreetEdge e, Coordinate p, double pref, TraverseModeSet mode) {
    	preference = pref;
    	edge = e;
    	edgeCoords = e.getGeometry().getCoordinateSequence();
        numEdgeCoords = edgeCoords.size();
        nearestPointOnEdge = new Coordinate();
    	
    	// Initializes this.platform
    	calcPlatform(mode);
        // Initializes nearestPointOnEdge, nearestSegmentIndex, nearestSegmentFraction.
    	distance = calcNearestPoint(p);
    	
    	// 
    	endwiseVertex = calcEndwiseVertex();
		score = calcScore();
        
		// Calculate the directional info.
		int edgeSegmentIndex = nearestSegmentIndex;
		Coordinate c0 = edgeCoords.getCoordinate(edgeSegmentIndex);
        Coordinate c1 = edgeCoords.getCoordinate(edgeSegmentIndex + 1);
		directionOfEdge = DirectionUtils.getAzimuth(c0, c1);
        directionToEdge = DirectionUtils.getAzimuth(nearestPointOnEdge, p);
        double absDiff = Math.abs(directionToEdge - directionOfEdge);
        directionDifference = Math.min(2 * Math.PI - absDiff, absDiff);
		if (Double.isNaN(directionToEdge) || Double.isNaN(directionOfEdge)
				|| Double.isNaN(directionDifference)) {
			StreetVertexIndexServiceImpl._log.warn(
					"direction to/of edge is NaN (0 length?): {}", edge);
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

    public String toString() {
        return "CE(" + edge + ", " + score + ")";
    }
    
    /**
     * Private methods
     */
    
    /**
     * Initializes this.nearestPointOnEdge and other distance-related variables.
     * 
     * @param p
     */
    private double calcNearestPoint(Coordinate p) {
    	LineString edgeGeom = edge.getGeometry();
        CoordinateSequence coordSeq = edgeGeom.getCoordinateSequence();
        int bestSeg = 0;
        double bestDist2 = Double.POSITIVE_INFINITY;
        double bestFrac = 0;
        double xscale = Math.cos(p.y * Math.PI / 180);
        for (int seg = 0; seg < numEdgeCoords - 1; seg++) {
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
        
        nearestSegmentIndex = bestSeg;
        nearestSegmentFraction = bestFrac;
        return Math.sqrt(bestDist2); // distanceLibrary.distance(p, nearestPointOnEdge);
    }
    

    /**
     * 
     * @return
     */
    private StreetVertex calcEndwiseVertex() {
    	StreetVertex retV = null;
		if (nearestSegmentIndex == 0 && Math.abs(nearestSegmentFraction) < 0.000001) {
			retV = (StreetVertex) edge.getFromVertex();
		} else if (nearestSegmentIndex == numEdgeCoords - 2
				   && Math.abs(nearestSegmentFraction - 1.0) < 0.000001) {
			retV = (StreetVertex) edge.getToVertex();
		} 
		return retV;
    }
    
    /**
     * Calculate the platform int.
     * 
     * @param mode
     * @return
     */
    private void calcPlatform(TraverseModeSet mode) {
    	int out = 0;
        if (mode.getTrainish()) {
            out |= StreetEdge.CLASS_TRAIN_PLATFORM;
        }
        if (mode.getBusish()) {
            out |= StreetEdge.CLASS_OTHER_PLATFORM;
        }
        platform = out;
	}

    /**
     * Internal calculator for the score.
     * 
     * Assumes that edge, platform and distance are initialized.
     * TODO(flamholz): account for direction of travel here.
     * 
     * @return
     */
	private double calcScore() {
		double myScore = 0;

		myScore = distance * SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M
				/ 360.0;
		myScore /= preference;
		if ((edge.getStreetClass() & platform) != 0) {
			// this a hack, but there's not really a better way to do it
			myScore /= PLATFORM_PREFERENCE;
		}
		if (edge.getName().contains("sidewalk")) {
			// this is a hack, but there's not really a better way to do it
			myScore /= SIDEWALK_PREFERENCE;
		}
		if (edge.getPermission().allows(StreetTraversalPermission.CAR)
				|| (edge.getStreetClass() & platform) != 0) {
			// we're subtracting here because no matter how close we are to a
			// good non-car non-platform edge, we really want to avoid it in
			// case it's a Pedway or other weird and unlikely starting location.
			myScore -= CAR_PREFERENCE;
		}
		
		// break ties by choosing shorter edges; this should cause split streets
		// to be preferred
		myScore += edge.getLength() / 1000000;
    	
    	return myScore;
    }
	
}