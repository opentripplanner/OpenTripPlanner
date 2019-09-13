package org.opentripplanner.analyst.batch.aggregator;

import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.ResultSet;

/**
 * An Aggregator which calculates the weighted average of the shortest path lengths to all Individuals in the destination Population.
 * 
 * This can be used to find the average distance/time to all people or jobs in a metropolitan area from a given origin.
 * 
 * @author andrewbyrd
 */
public class WeightedAverageAggregator implements Aggregator {

    @Override
    public double computeAggregate(ResultSet rs) {
        double aggregate = 0;
        int i = 0;
        int n = 0;
        for (Individual target: rs.population) {
            double t = rs.results[i++];
            if (Double.isInfinite(target.input))
                continue;
            if (Double.isInfinite(t) || t < 0)
                continue;
            aggregate += target.input * t;
            n += target.input;
        }
        aggregate /= n;
        return aggregate;
    }

}
