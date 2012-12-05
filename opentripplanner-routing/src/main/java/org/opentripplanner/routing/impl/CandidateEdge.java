package org.opentripplanner.routing.impl;

import lombok.Getter;

import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.LocationObservation;
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
	
	private static final double MAX_DIRECTION_DIFFERENCE = 180.0;
	
	private static final double MAX_ABS_DIRECTION_DIFFERENCE = 360.0;

	/**
	 * The edge itself.
	 */
	@Getter
	protected final StreetEdge edge;

	/**
	 * Pointer to the coordinates of the edge.
	 */
	private final CoordinateSequence edgeCoords;

	/**
	 * Number of coordinates on the edge.
	 */
	private final int numEdgeCoords;

	/**
	 * Whether point is located at a platform.
	 */
	private final int platform;

	/**
	 * Preference value passed in.
	 */
	private final double preference;

	/**
	 * Index of the closest segment along the edge.
	 */
	private int nearestSegmentIndex;

	/**
	 * Fractional distance along the closest segment.
	 */
	private double nearestSegmentFraction;

	/**
	 * Set when to the closest endpoint of the edge when the input location is
	 * really sitting on that endpoint (within some tolerance).
	 */
	@Getter
	protected StreetVertex endwiseVertex;

	/**
	 * The coordinate of the nearest point on the edge.
	 */
	@Getter
	protected Coordinate nearestPointOnEdge;

	/**
	 * Heading if given.
	 */
	@Getter
	protected Double heading;

	/**
	 * Azimuth between input point and closest point on edge.
	 */
	@Getter
	protected double directionToEdge;
	
	/**
	 * Azimuth of the subsegment of the edge to which the input point is
	 * closest.
	 */
	@Getter
	protected double directionOfEdge;

	/**
	 * Difference in direction between heading and nearest subsegment of edge.
	 * Null if no heading given.
	 */
	@Getter
	protected Double directionDifference;

	/**
	 * Distance from edge and point.
	 */
	@Getter
	protected double distance;

	/**
	 * Score of the match. Lower is better.
	 */
	@Getter
	protected double score;

	/**
	 * Construct from a LocationObservation. 
	 * 
	 * @param e
	 * @param loc
	 * @param pref
	 * @param mode
	 */
	public CandidateEdge(StreetEdge e, LocationObservation loc, double pref,
			TraverseModeSet mode) {
		preference = pref;
		edge = e;
		edgeCoords = e.getGeometry().getCoordinateSequence();
		numEdgeCoords = edgeCoords.size();
		platform = calcPlatform(mode);		
		nearestPointOnEdge = new Coordinate();

		// Initializes nearestPointOnEdge, nearestSegmentIndex,
		// nearestSegmentFraction.
		distance = calcNearestPoint(loc.getCoordinate());

		// Calculates the endwise vertex as appropriate.
		endwiseVertex = calcEndwiseVertex();

		// Calculate the directional info.
		int edgeSegmentIndex = nearestSegmentIndex;
		Coordinate c0 = edgeCoords.getCoordinate(edgeSegmentIndex);
		Coordinate c1 = edgeCoords.getCoordinate(edgeSegmentIndex + 1);
		directionOfEdge = DirectionUtils.getAzimuth(c0, c1);
		directionToEdge = DirectionUtils.getAzimuth(nearestPointOnEdge,
				loc.getCoordinate());

		// Calculates the direction differently depending on whether a heading
		// is supplied.
		heading = loc.getHeading();
		if (heading != null) {
			double absDiff = Math.abs(heading - directionOfEdge);
			directionDifference = Math.min(
					MAX_ABS_DIRECTION_DIFFERENCE - absDiff, absDiff);
		}

		// Calculate the score last so it can use all other data.
		score = calcScore();
	}
	
	/**
	 * Construct from a Coordinate. 
	 * 
	 * Deprecated.
	 * 
	 * @param e
	 * @param p
	 * @param pref
	 * @param mode
	 */
	public CandidateEdge(StreetEdge e, Coordinate p, double pref,
			TraverseModeSet mode) {
		this(e, new LocationObservation(p), pref, mode);
	}

	
	public boolean endwise() {
		return endwiseVertex != null;
	}

	public String toString() {
		return String
				.format("CandidateEdge<edge=\"%s\" score=\"%f\" heading=\"%s\" directionDifference=\"%s\">",
						edge, score, heading, directionDifference);
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
			double x1 = coordSeq.getX(seg + 1);
			double y1 = coordSeq.getY(seg + 1);
			double frac = GeometryUtils.segmentFraction(x0, y0, x1, y1, p.x,
					p.y, xscale);
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
		return Math.sqrt(bestDist2); // distanceLibrary.distance(p,
										// nearestPointOnEdge);
	}

	/**
	 * Calculates the endwiseVertex if appropriate.
	 * 
	 * @return
	 */
	private StreetVertex calcEndwiseVertex() {
		StreetVertex retV = null;
		if (nearestSegmentIndex == 0
				&& Math.abs(nearestSegmentFraction) < 0.000001) {
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
	private int calcPlatform(TraverseModeSet mode) {
		int out = 0;
		if (mode.getTrainish()) {
			out |= StreetEdge.CLASS_TRAIN_PLATFORM;
		}
		if (mode.getBusish()) {
			out |= StreetEdge.CLASS_OTHER_PLATFORM;
		}
		return out;
	}

	/**
	 * Internal calculator for the score.
	 * 
	 * Assumes that edge, platform and distance are initialized. TODO(flamholz):
	 * account for direction of travel here.
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

		// Consider the heading in the score if it is available.
		if (heading != null) {
			// If you are moving along the edge, score is not penalized.
			// If you are moving against the edge, score is penalized by 1.
			myScore += (directionDifference / MAX_DIRECTION_DIFFERENCE);
		}
		
		// break ties by choosing shorter edges; this should cause split streets
		// to be preferred
		myScore += edge.getLength() / 1000000;

		return myScore;
	}
}