package org.opentripplanner.routing.manytomany;

/**
 * An Aggregator which calculates the weighted average of the shortest path
 * lengths to all Individuals in the destination Population.
 * 
 * This can be used to find the average distance/time to all people or jobs in a
 * metropolitan area from a given origin.
 * 
 * @author andrewbyrd
 */
public class WeightedAverageAggregator implements Aggregator {

	@Override
	public double computeAggregate(Population destinations) {
		double result = 0;
		int n = 0;
		for (Individual destination : destinations.elements) {
			if (Double.isInfinite(destination.data))
				continue;
			if (Double.isInfinite(destination.result))
				continue;

			result += destination.data * destination.result;
			n += destination.data;
		}
		result /= n;
		return result;
	}

}
