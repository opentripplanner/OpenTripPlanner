package org.opentripplanner.routing.manytomany;

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
	public double computeAggregate(Population destinations) {
		double result = 0;
		for (Individual destination : destinations.elements)
			if (destination.result < thresholdSeconds)
				result += destination.data;

		return result;
	}

}
