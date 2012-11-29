package org.opentripplanner.api.thrift.util;

import org.opentripplanner.api.thrift.definition.LatLng;

import com.vividsolutions.jts.geom.Coordinate;

public class LatLngExtension extends LatLng {

	/**
	 * Default constructor.
	 * 
	 * TODO(flamholz): figure out if lombok calls superclass constructor?
	 */
	public LatLngExtension() {
		super();
	}

	/**
	 * Makes a Thrift LatLng structure from a Coordinate.
	 * 
	 * @param c
	 * @return
	 */
	public LatLngExtension(Coordinate c) {
		this();
		setCoordinate(c);
	}

	/**
	 * Set lat and lng correctly mapping y, x = lat, lng
	 * 
	 * @param c
	 */
	public void setCoordinate(Coordinate c) {
		// lat, lng = y, x
		setLat(c.y);
		setLng(c.x);
	}
}
