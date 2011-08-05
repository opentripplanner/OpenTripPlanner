package org.opentripplanner.routing.manytomany;

/**
 * An interface for classes that provide an aggregate function over populations
 * of places. Can be used to provide various accessibility calculations, for example.
 * 
 * @author andrewbyrd
 */
public interface Aggregator {
	public double computeAggregate(Population destinations);
}
