package org.opentripplanner.api.thrift.util;

import org.opentripplanner.api.thrift.definition.EdgeMatch;
import org.opentripplanner.routing.impl.CandidateEdge;


public class EdgeMatchExtension extends EdgeMatch {
	
	/**
	 * Required for serialization.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Construct from candidate edge.
	 * 
	 * @param candidate
	 */
	public EdgeMatchExtension(CandidateEdge candidate) {
		setEdge(new GraphEdgeExtension(candidate.edge));
		setClosest_point(new LatLngExtension(candidate.nearestPointOnEdge));
		setScore(candidate.getScore());
	}
}
