package org.opentripplanner.analyst.batch.aggregator;

import lombok.Setter;

import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.Population;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 * An Aggregator that simply sums the data for all destination Individuals less than a given distance/weight away from the origin point. This can be
 * used for simple cumulative opportunity accessibility indicators.
 * 
 * @author andrewbyrd
 */
public class ThresholdSumAggregator implements Aggregator {

    @Setter int threshold = 60 * 90; // 1.5 hours in seconds

    @Override
    public double computeAggregate(Population destinations) {
        double result = 0;
        for (Individual destination : destinations)
            if (destination.output > 0 && destination.output < threshold)
                result += destination.input;

        return result;
    }

}
