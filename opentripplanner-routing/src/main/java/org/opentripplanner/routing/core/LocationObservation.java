package org.opentripplanner.routing.core;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Instantaneous observation of a user/vehicle location.
 * 
 * Mimics Observation class in tracking tools.
 * 
 * @author avi
 */
@Getter @AllArgsConstructor
public class LocationObservation {
	
	// Timestamp of the observation.
	private final Date timestamp;
	
	// Location of the observation.
	private final Coordinate coordinate;

	// Accuracy of coordinate in meters.
	private final Double accuracy;
		
	// Observed instantaneous velocity if any.
	private final Double velocity;
	
	// Observed heading if any.
	private final Double heading;
	
	/**
	 * Construct with just a Coordinate.
	 * 
	 * @param c
	 */
	public LocationObservation(Coordinate c) {
		timestamp = null;
		coordinate = c;
		accuracy = null;
		velocity = null;
		heading = null;
	}

	/**
	 * Construct with a timestamp and Coordinate.
	 * 
	 * @param t
	 * @param c
	 */
	public LocationObservation(Date t, Coordinate c) {
		timestamp = t;
		coordinate = c;
		accuracy = null;
		velocity = null;
		heading = null;
	}
}