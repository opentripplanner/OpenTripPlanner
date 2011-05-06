package org.opentripplanner.routing.algorithm.strategies;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.spt.SPTVertex;

public interface RemainingWeightHeuristic {

    public double computeInitialWeight(Vertex from, Vertex to, TraverseOptions traverseOptions);

    public double computeForwardWeight(SPTVertex from, Edge edge, TraverseResult traverseResult,
            Vertex target);

    public double computeReverseWeight(SPTVertex from, Edge edge, TraverseResult traverseResult,
            Vertex target);
}
