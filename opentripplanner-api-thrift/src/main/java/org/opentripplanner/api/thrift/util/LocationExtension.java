package org.opentripplanner.api.thrift.util;

import org.opentripplanner.api.thrift.definition.LatLng;
import org.opentripplanner.api.thrift.definition.Location;

import com.vividsolutions.jts.geom.Coordinate;

public class LocationExtension extends Location {

	/**
	 * Default constructor.
	 * 
	 * TODO(flamholz): figure out if lombok calls superclass constructor?
	 */
	public LocationExtension() {
		super();
	}

	/**
	 * Makes a Thrift Location structure from a Coordinate.
	 * 
	 * @param c
	 * @return
	 */
	public LocationExtension(Coordinate c) {
		this();
		setCoordinate(c);
	}

	/**
	 * Set the internal LatLng to a Coordinate, correctly mapping y, x = lat,
	 * lng
	 * 
	 * @param c
	 */
	public void setCoordinate(Coordinate c) {
		// lat, lng = y, x
		LatLng latlng = new LatLng(c.y, c.x);
		this.setLat_lng(latlng);
	}
}
