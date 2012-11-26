package org.opentripplanner.api.thrift.util;

import java.util.Set;

import org.opentripplanner.api.thrift.definition.LatLng;
import org.opentripplanner.api.thrift.definition.Location;
import org.opentripplanner.api.thrift.definition.TravelMode;
import org.opentripplanner.api.thrift.definition.TripParameters;
import org.opentripplanner.routing.core.RoutingRequest;


/**
 * Utilities for processing trip data.
 * @author avi
 */
public class TripUtil {

	/**
	 * Convert a LatLng structure into an internal String representation.
	 * @param ll
	 * @return String that is accepted internally as a LatLng.
	 */
	private static String latLngToString(final LatLng latlng) {
		// NOTE: 7 decimal places means better than cm resolution.
		return String.format("%.7f,%.7f", latlng.getLat(), latlng.getLng());
	}
	
	/**
	 * Initializes a routing request from TripParameters structure.
	 * @param tripParams
	 * @return partially initialized RoutingRequest
	 */
	public static RoutingRequest initRoutingRequest(final TripParameters tripParams) {
		RoutingRequest options = new RoutingRequest();
		
		if (tripParams.isSetAllowed_modes()) {
			Set<TravelMode> allowedModes = tripParams.getAllowed_modes();
			TravelModeSet travelModeSet = new TravelModeSet(allowedModes);
			options.setModes(travelModeSet.toTraverseModeSet());			
		}
		
		Location origin = tripParams.getOrigin();
		Location destination = tripParams.getDestination();
		options.setFrom(latLngToString(origin.getLat_lng()));
		options.setTo(latLngToString(destination.getLat_lng()));
				
		return options;
	}
	
}
