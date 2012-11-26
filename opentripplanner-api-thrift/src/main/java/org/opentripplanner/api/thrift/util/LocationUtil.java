package org.opentripplanner.api.thrift.util;

import org.opentripplanner.api.thrift.definition.LatLng;
import org.opentripplanner.api.thrift.definition.Location;

import com.vividsolutions.jts.geom.Coordinate;

public class LocationUtil {

	/**
	 * Makes a Thrift Location structure from a Coordinate.
	 * @param c
	 * @return
	 */
	public static Location makeLocation(Coordinate c) {
		Location loc = new Location();
		// lat, lng = y, x
		LatLng latlng = new LatLng(c.y, c.x);
		loc.setLat_lng(latlng);
		return loc;
	}
}
