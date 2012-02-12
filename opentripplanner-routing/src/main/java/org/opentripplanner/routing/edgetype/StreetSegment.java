package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.TraverseOptions;

/*
 * StreetSegment will hold the information for a single piece of street covered by around six 
 * TurnEdges. It should replace StreetVertex, and be refrenced from the edges themselves.
 * 
 * It should be subclassed for specific kinds of street segments, like stairs or roundabouts.
 */
public interface StreetSegment {

    public double getWeight(TraverseOptions options);

}
