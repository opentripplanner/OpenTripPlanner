package org.opentripplanner.routing.core;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
	
	/**
	 * Observed heading if any.
	 * 
	 * Direction of travel in decimal degrees from -180° to +180° relative to
	 * true north.
	 * 
	 * 0      = heading true north.
	 * +/-180 = heading south.
	 */
	private final Double heading;
	
	/**
	 * Builder provided since everything is final in LocationObservation.
	 * 
	 * @author avi
	 */
	@AllArgsConstructor @NoArgsConstructor
	public static class Builder {
		private Date timestamp;
		private Coordinate coordinate;
		private Double accuracy;
		private Double velocity;
		private Double heading;
		
		public Builder setTimestamp(Date ts) {
			timestamp = ts;
			return this;
		}
		
		public Builder setCoordinate(Coordinate c) {
			coordinate = c;
			return this;
		}
		
		public Builder setAccuracy(Double a) {
			accuracy = a;
			return this;
		}
		
		public Builder setVelocity(Double v) {
			velocity = v;
			return this;
		}
		
		public Builder setHeading(Double h) {
			heading = h;
			return this;
		}
		
		public LocationObservation build() {
			return new LocationObservation(timestamp, coordinate, accuracy, velocity, heading);
		}
	}
	
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