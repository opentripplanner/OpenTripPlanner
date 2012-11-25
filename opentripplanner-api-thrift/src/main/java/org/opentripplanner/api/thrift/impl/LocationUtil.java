package org.opentripplanner.api.thrift.impl;

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
		LatLng ll = new LatLng(c.y, c.x);
		loc.setLat_lng(ll);
		return loc;
	}
}
