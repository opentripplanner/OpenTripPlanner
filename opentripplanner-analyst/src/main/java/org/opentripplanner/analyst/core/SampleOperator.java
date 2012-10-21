package org.opentripplanner.analyst.core;

import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.Population;
import org.opentripplanner.analyst.batch.ResultSet;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 * Each implementation or instance of a SampleOperator provides custom logic for converting the 
 * States pulled from a ShortestPathTree into numbers. This allows pluggable behavior, so images
 * or other Analyst results can show travel time with or without initial wait time, number of 
 * transfers, weight, or any other path characteristic. 
 * 
 * You can imagine this as an infix operator:
 * ShortestPathTree [SampleOperator] SampleSet => ResultSet 
 * 
 * i.e. a SampleOperator produces a ResultSet by combining a ShortestPathTree with a SampleSet.
 * 
 * @author abyrd
 */
public abstract class SampleOperator {

    /** If true, report smaller values in results. If false, report greater values. */
    protected boolean minimize = true;
    
    /** Implement this method to supply logic for converting States to result numbers */
    public abstract int evaluate(State state, double distance);

    public ResultSet evaluate(ShortestPathTree spt, Population population) {
        double[] results = new double[population.size()];
        
        // replace Sample with Sample[] or Pair<Sample>?
        int i = 0;
        for (Individual indiv : population) { // iterate over samples that have not been filtered out
            int extreme = minimize ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            int bestResult = extreme;
            // if skip[i]...
            Sample s = indiv.sample;
            // add null s0s1 checks
            State s0 = spt.getState(s.v0);
            State s1 = spt.getState(s.v1);
            if (s0 != null)
                bestResult = evaluate(s0, s.t0);
            if (s1 != null) {
                int r = evaluate(s1, s.t1);
                if (minimize) {
                    if (r < bestResult)
                        bestResult = r;
                } else {
                    if (r > bestResult)
                        bestResult = r;
                }
            }
            results[i] = (bestResult == extreme) ? Integer.MAX_VALUE : bestResult;
        }
        // maybe change result sets to use ints instead of doubles? do we need FP values?
        return new ResultSet(population, results);
    }
    
}
