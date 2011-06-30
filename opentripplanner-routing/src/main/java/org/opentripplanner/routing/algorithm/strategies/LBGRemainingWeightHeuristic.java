package org.opentripplanner.routing.algorithm.strategies;

import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.LowerBoundGraph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intended primarily for testing of and experimentation with heuristics based on
 * the triangle inequality and metric embeddings.
 * 
 * A heuristic that performs a single-source / all destinations shortest path search
 * in a weighted, directed graph whose shortest path metric is a lower bound on
 * path weight in our main, time-dependent graph. 
 * 
 * @author andrewbyrd
 */
public class LBGRemainingWeightHeuristic implements RemainingWeightHeuristic {
    private static Logger LOG = LoggerFactory.getLogger(LBGRemainingWeightHeuristic .class);

	LowerBoundGraph lbg;
	Vertex target;
	double[] weights;
	
	public LBGRemainingWeightHeuristic (Graph g) {
		LOG.debug("BEGIN Making lower bound graph");
		this.lbg = new LowerBoundGraph(g, LowerBoundGraph.OUTGOING);
		LOG.debug("END   Making lower bound graph");
	}
	
    @Override
    public double computeInitialWeight(State s, Vertex target) {
    	recalculate(target);
    	return 0;
    }

    @Override
    public double computeForwardWeight(State s, Vertex target) {
        return computeReverseWeight(s, target);
    }

    @Override
    public double computeReverseWeight(State s, Vertex target) {
    	//return 0;
    	int index = ((GenericVertex)s.getVertex()).getIndex();
        if (index < weights.length) {
        	double h = weights[index];
        	//System.out.printf("h=%f at %s\n", h, s.getVertex());
        	return h == Double.POSITIVE_INFINITY ? 0 : h;
        } else return 0;
    }

    private void recalculate(Vertex target) {
    	if (target != this.target) {
        	this.target = target;
    		this.weights = lbg.sssp((StreetLocation)target);
    	}
    }

	@Override
	public void reset() {		
	}
}
