package org.opentripplanner.analyst.batch.aggregator;

import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.Population;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 * An Aggregator that simply sums the data for all destination Individuals less
 * than a given distance/weight away from the origin point. This can be used for
 * simple cumulative opportunity accessibility indicators.
 * 
 * @author andrewbyrd
 */
public class ThresholdSumAggregator implements Aggregator {

	int thresholdSeconds = 0;

	public ThresholdSumAggregator(int thresholdSeconds) {
		this.thresholdSeconds = thresholdSeconds;
	}

	@Override
	public double computeAggregate(Population destinations, ShortestPathTree spt) {
		double result = 0;
		for (Individual destination : destinations)
			if (destination.sample.eval(spt) < thresholdSeconds)
				result += destination.data;

		return result;
	}

}
