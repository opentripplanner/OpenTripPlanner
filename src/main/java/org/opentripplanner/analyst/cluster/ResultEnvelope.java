package org.opentripplanner.analyst.cluster;

import org.opentripplanner.analyst.ResultSet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a class that stores several result sets: an upper bound, a lower bound, and a central tendency.
 * This is an "Envelope" in the sense that the results it contains enclose an area on a map or a chart:
 * either the space between the minimum and maximum extent of an N-minute isochrone, or the space between the minimum
 * and maximum accessibility curves on the cumulative access chart.
 *
 * @author mattwigway
 */
public class ResultEnvelope implements Serializable {

	/**
	 * The best case/upper bound (e.g. number of jobs reachable considering the best travel times seen during the
	 * time window)
	 */
	public ResultSet bestCase;
	
	/**
	 * The lower bound (e.g. number of jobs reachable considering the worst travel times seen during the time window)
	 */
	public ResultSet worstCase;

	/**
	 * The average case (e.g. the number of jobs reachable on average, departing randomly within the time window)
	 */
	public ResultSet avgCase;
	
	/**
	 * A point estimate of accessibility, in the statistical sense: a single result that serves as a representative point
	 * in a distribution. When we are not doing profile routing (when we are not varying the departure time or other
	 * characteristics such as frequency board delays or schedule pulsing) we get only a single set of travel
	 * times out of a search. This is only one draw out of a distribution, and we don't know how much the values
	 * can vary around this single example.
	 *
	 * Note that this is not a single travel time, but a single set of travel times to all targets (points/vertices).
	 */
	public ResultSet pointEstimate;
	
	/**
	 * The spread of results (future use). This could be the standard deviation or
	 * interquartile range, for example. 
	 */
	public ResultSet spread;
	
	/**
	 * Is this a profile request?
	 * If so, upperBound and lowerBound will be defined, and pointEstimate not.
	 * Otherwise, only pointEstimate will be defined.
	 */
	public boolean profile;

	/** The ID of the job of which this resultenvelope is a part */
	public String jobId;

	/** The ID of the feature from whence this result envelope came */
	public String id;
	
	/** The ID of the shapefile/pointset from whence this result envelope came */
	public String destinationPointsetId;
	
	public ResultSet get (Which key) {
		switch (key) {
		case BEST_CASE:
			return this.bestCase;
		case WORST_CASE:
			return this.worstCase;
		case POINT_ESTIMATE:
			return this.pointEstimate;
		case SPREAD:
			return this.spread;
		case AVERAGE:
			return this.avgCase;
		default:
			throw new IllegalStateException("Invalid result type!");
		}
	}
	
	public void put (Which key, ResultSet val) {
		switch (key) {
		case BEST_CASE:
			this.bestCase = val;
			break;
		case WORST_CASE:
			this.worstCase = val;
			break;
		case POINT_ESTIMATE:
			this.pointEstimate = val;
			break;
		case SPREAD:
			this.spread = val;
			break;
		case AVERAGE:
			this.avgCase = val;
			break;
		}
	}
	
	/**
	 * Explode this result envelope into a result envelope for each contained attribute
	 * We do this because we need to retrieve all of the values for a particular variable quickly, in order to display the map,
	 * and looping over 10GB of data to do this is not tractable.
	 */
	public Map<String, ResultEnvelope> explode () {
		Map<String, ResultEnvelope> exploded = new HashMap<String, ResultEnvelope>();
		
		// find a non-null resultset
		for (String attr : (pointEstimate != null ? pointEstimate : avgCase).histograms.keySet()) {
			ResultEnvelope env = new ResultEnvelope();
			
			for (Which which : Which.values()) {
				ResultSet orig = this.get(which);
				
				if (orig != null) {
					ResultSet rs = new ResultSet();
					rs.id = orig.id;
					rs.histograms.put(attr, orig.histograms.get(attr));
					env.put(which, rs);
					env.id = this.id;
				}
			}
			
			exploded.put(attr, env);
		}
		
		return exploded;
	}
	
	/**
	 * Build an empty result envelope.
	 */
	public ResultEnvelope () {
		// do nothing, restore default constructor
	}
	
	public static enum Which {
		BEST_CASE, WORST_CASE, POINT_ESTIMATE, SPREAD, AVERAGE;

		/** Return a human readable string of this value */
		public String toHumanString () {
			switch (this) {
				case BEST_CASE:
					return "best case";
				case WORST_CASE:
					return "worst case";
				case POINT_ESTIMATE:
					return "point estimate";
				case SPREAD:
					return "spread";
				case AVERAGE:
					return "average";
				default:
					throw new IllegalStateException("Unknown envelope parameter");
			}
		}
	}
}
