package org.opentripplanner.routing.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import com.vividsolutions.jts.geom.Coordinate;

@Data @AllArgsConstructor
public class LocationInformation {

	public static class TimedLocation {
		
		/**
		 * Timestamp of when the location is from.
		 * 
		 * Optional. In milliseconds since the epoch.
		 */
		@Getter
		private long timestampMillis = -1;
		
		/**
		 * Coordinate locating a person/vehicle
		 */
		@Getter
		private Coordinate coordinate;
		
		/**
		 * Direction of travel as of timestamp.
		 */
		@Getter
		private double bearingAzimuth; 
		private boolean hasBearingAzimuth = false;
		
		/**
		 * Initialize with just a coordinate;
		 * @param c
		 */
		public TimedLocation(Coordinate c) {
			coordinate = c;
		}
		
		/**
		 * Initialize with all relevant arguments.
		 * @param c
		 */
		public TimedLocation(long timestampMillis, Coordinate c,
				double bearingAzimuth) {
			this(c);
			this.timestampMillis = timestampMillis;
			this.bearingAzimuth = bearingAzimuth;
		}
		
		/**
		 * Get the timestamp of this location in seconds.
		 * @return
		 */
		public long getTimestampSeconds() {
			return timestampMillis / 1000;
		}
		
		/**
		 * Returns true if this hass a timestamp.
		 * 
		 * @return
		 */
		public boolean hasTimestamp() {
			return timestampMillis >= 0;
		}
		
		public boolean hasBearingAzimuth() {
			return hasBearingAzimuth;
		}
	}
	
	/**
	 * Current location of the person/vehicle.
	 */
	private TimedLocation currentLocation;
	
	/**
	 * Initialize with just a single coordinate.
	 * 
	 * @param c
	 */
	public LocationInformation(Coordinate c) {
		currentLocation = new TimedLocation(c);
	}
	
}
