package org.opentripplanner.analyst.batch.aggregator;

import lombok.Setter;

import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.ResultSet;

/**
 * An Aggregator that simply sums the data for all destination Individuals less than a given distance/weight away from the origin point. This can be
 * used for simple cumulative opportunity accessibility indicators.
 * 
 * @author andrewbyrd
 */
public class ThresholdSumAggregator implements Aggregator {

    @Setter int threshold = 60 * 90; // 1.5 hours in seconds

    @Override
    public double computeAggregate(ResultSet rs) {
        double aggregate = 0;
        int i = 0;
        for (Individual target : rs.population) {
            double t = rs.results[i];
            if (t > 0 && t < threshold)
                aggregate += target.input;
            i++;
        }
        return aggregate;
    }

}
