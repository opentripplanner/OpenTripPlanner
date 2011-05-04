package org.opentripplanner.routing.error;

/**
 * Indicates that a transit mode was specified, but the search date provided 
 * was outside the date range of the transit data feed used to construct the graph.
 * In other words, there is no transit service information available, 
 * and the user needs to be told this.  
 */
public class TransitTimesException extends RuntimeException {

	private static final long serialVersionUID = 1L;

}
