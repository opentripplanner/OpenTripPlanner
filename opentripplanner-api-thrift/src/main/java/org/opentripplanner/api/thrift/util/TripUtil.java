package org.opentripplanner.api.thrift.util;

import org.opentripplanner.api.thrift.definition.LatLng;
import org.opentripplanner.api.thrift.definition.Location;
import org.opentripplanner.api.thrift.definition.TravelMode;
import org.opentripplanner.api.thrift.definition.TripParameters;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;


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
	public static String latLngToString(final LatLng ll) {
		return String.format("%f,%f", ll.getLat(), ll.getLng());
	}
	
	/**
	 * Convert a TravelMode enum (external) to a TraverseMode (internal).
	 * @param m
	 * @return TraverseMode value
	 */
	public static TraverseMode getTraverseMode(final TravelMode m){
		switch (m) {
		case BICYCLE:
			return TraverseMode.BICYCLE;
		case WALK:
			return TraverseMode.WALK;
		case CAR:
			return TraverseMode.CAR;
		case TRAM:
			return TraverseMode.TRAM;
		case SUBWAY:
			return TraverseMode.SUBWAY;
		case RAIL:
			return TraverseMode.RAIL;
		case ANY_TRAIN:
			return TraverseMode.TRAINISH;
		case ANY_TRANSIT:
			return TraverseMode.TRANSIT;
		default:
			return null;
		}
	}
	
	/**
	 * Initializes a routing request from TripParameters structure.
	 * @param tripParams
	 * @return partially initialized RoutingRequest
	 */
	public static RoutingRequest initRoutingRequest(final TripParameters tripParams) {
		RoutingRequest options = new RoutingRequest();
		
		if (tripParams.isSetAllowed_modes()) {
			options.clearModes();
			
			for (TravelMode m : tripParams.getAllowed_modes()) {
				TraverseMode tm = getTraverseMode(m);
				if (tm != null) options.addMode(tm);
			}
		}
		
		Location origin = tripParams.getOrigin();
		Location destination = tripParams.getDestination();
		options.setFrom(latLngToString(origin.getLat_lng()));
		options.setTo(latLngToString(destination.getLat_lng()));
				
		return options;
	}
	
}
