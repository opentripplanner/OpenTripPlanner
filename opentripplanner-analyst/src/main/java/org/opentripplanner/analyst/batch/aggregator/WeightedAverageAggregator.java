package org.opentripplanner.analyst.batch.aggregator;

import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.Population;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 * An Aggregator which calculates the weighted average of the shortest path lengths to all Individuals in the destination Population.
 * 
 * This can be used to find the average distance/time to all people or jobs in a metropolitan area from a given origin.
 * 
 * @author andrewbyrd
 */
public class WeightedAverageAggregator implements Aggregator {

    @Override
    public double computeAggregate(Population destinations) {
        double result = 0;
        int n = 0;
        for (Individual destination : destinations) {
            if (Double.isInfinite(destination.input))
                continue;
            double t = destination.output;
            if (Double.isInfinite(t) || t < 0)
                continue;
            result += destination.input * t;
            n += destination.input;
        }
        result /= n;
        return result;
    }

}
