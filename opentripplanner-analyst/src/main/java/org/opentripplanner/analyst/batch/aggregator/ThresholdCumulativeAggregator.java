package org.opentripplanner.analyst.batch.aggregator;

import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.Population;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 * An aggregator that approximates the integral of a cumulative opportunity curve up to a certain threshold distance from the origin. This is vaguely
 * inspired by the Lorenz curve and the Gini coefficient, and is intended to be a measure of urban centrality. Opportunities that are closer to the
 * search origin will be weighted much more heavily than those nearer to the threshold.
 * 
 * @author andrewbyrd
 */
public class ThresholdCumulativeAggregator implements Aggregator {

    int thresholdSeconds = 0;

    public ThresholdCumulativeAggregator(int thresholdSeconds) {
        this.thresholdSeconds = thresholdSeconds;
    }

    @Override
    public double computeAggregate(Population destinations) {
        double result = 0;
        for (Individual destination : destinations) {
            double t = destination.output;
            if (t > 0 && t < thresholdSeconds)
                result += destination.input * (thresholdSeconds - t);
        }
        return result;
    }

}
