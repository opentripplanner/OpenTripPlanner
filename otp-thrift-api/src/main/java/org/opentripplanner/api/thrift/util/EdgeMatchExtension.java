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
        setEdge(new GraphEdgeExtension(candidate.getEdge()));
        setClosest_point(new LatLngExtension(candidate.getNearestPointOnEdge()));
        setScore(candidate.getScore());
        setDistance_from_query(candidate.getDistance());
        setHeading_at_closest_point(candidate.getDirectionOfEdge());
    }
}
