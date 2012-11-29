package org.opentripplanner.api.thrift.util;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.opentripplanner.api.thrift.definition.LatLng;
import org.opentripplanner.api.thrift.definition.Location;
import org.opentripplanner.api.thrift.definition.TravelMode;
import org.opentripplanner.api.thrift.definition.TripParameters;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;

import lombok.NoArgsConstructor;

/**
 * Builder for RoutingRequests.
 * 
 * @author avi
 */
@NoArgsConstructor
public class RoutingRequestBuilder {

	private final RoutingRequest routingRequest = new RoutingRequest();
	private Graph graph;

	/**
	 * Initialize with given TripParameters.
	 * 
	 * @param tripParams
	 */
	public RoutingRequestBuilder(TripParameters tripParams) {
		addTripParameters(tripParams);
	}

	/**
	 * Convert a LatLng structure into an internal String representation.
	 * 
	 * TODO(flamholz): put this on something inheriting from LatLng?
	 * 
	 * @param latlng
	 * @return String that is accepted internally as a LatLng.
	 */
	private static String latLngToString(final LatLng latlng) {
		// NOTE: 7 decimal places means better than cm resolution.
		return String.format("%.7f,%.7f", latlng.getLat(), latlng.getLng());
	}

	/**
	 * Sets the trip start time.
	 * 
	 * @param startTime seconds since the epoch.
	 * @return
	 */
	public RoutingRequestBuilder setStartTime(long startTime) {
		routingRequest.dateTime = startTime;
		routingRequest.setArriveBy(false);
		return this;
	}
	
	/**
	 * Set the requested arrival time of the trip.
	 * 
	 * @param arriveBy seconds since the epoch.
	 * @return
	 */
	public RoutingRequestBuilder setArriveBy(long arriveBy) {
		routingRequest.dateTime = arriveBy;
		routingRequest.setArriveBy(true);
		return this;
	}
	
	/**
	 * Adds TripParameters to the RoutingRequest.
	 * 
	 * @param tripParams
	 * @return self reference
	 */
	public RoutingRequestBuilder addTripParameters(TripParameters tripParams) {
		if (tripParams.isSetAllowed_modes()) {
			Set<TravelMode> allowedModes = tripParams.getAllowed_modes();
			setTravelModes(new TravelModeSet(allowedModes));
		}

		// Set trip timing information
		if (tripParams.isSetStart_time()) {
			setStartTime(tripParams.getStart_time());
		} else if (tripParams.isSetArrive_by()) {
			setArriveBy(tripParams.getArrive_by());
		}
		
		setOrigin(tripParams.getOrigin().getLat_lng());
		setDestination(tripParams.getDestination().getLat_lng());

		return this;
	}

	/**
	 * Overwrite the set of allowed TravelModes.
	 * 
	 * @param modes
	 * @return
	 */
	public RoutingRequestBuilder setTravelModes(TravelModeSet modes) {
		routingRequest.setModes(modes.toTraverseModeSet());
		return this;
	}
	
	/**
	 * Overwrite the set of allowed TravelModes.
	 * 
	 * @param modes
	 * @return
	 */
	public RoutingRequestBuilder setTravelModes(Collection<TravelMode> modes) {
		TravelModeSet modeSet = new TravelModeSet(modes);
		routingRequest.setModes(modeSet.toTraverseModeSet());
		return this;
	}

	/**
	 * Set the trip origin.
	 * 
	 * @param from
	 * @return self reference
	 */
	public RoutingRequestBuilder setOrigin(Location origin) {
		routingRequest.setFrom(latLngToString(origin.getLat_lng()));
		return this;
	}

	/**
	 * Set the trip origin.
	 * 
	 * @param from
	 * @return self reference
	 */
	public RoutingRequestBuilder setOrigin(LatLng origin) {
		routingRequest.setFrom(latLngToString(origin));
		return this;
	}

	/**
	 * Set the trip destination.
	 * 
	 * @param from
	 * @return self reference
	 */
	public RoutingRequestBuilder setDestination(Location dest) {
		routingRequest.setTo(latLngToString(dest.getLat_lng()));
		return this;
	}

	/**
	 * Set the trip destination.
	 * 
	 * @param from
	 * @return self reference
	 */
	public RoutingRequestBuilder setDestination(LatLng dest) {
		routingRequest.setTo(latLngToString(dest));
		return this;
	}

	/**
	 * Set whether to search in batch mode.
	 * 
	 * @param batch
	 * @return
	 */
	public RoutingRequestBuilder setBatch(boolean batch) {
		routingRequest.setBatch(batch);
		return this;
	}

	/**
	 * Set the graph to route on.
	 * 
	 * @param g
	 * @return self reference.
	 */
	public RoutingRequestBuilder setGraph(Graph g) {
		graph = g;
		return this;
	}

	/**
	 * Set the number of itineraries to return.
	 * 
	 * @param n
	 * @return self reference.
	 */
	public RoutingRequestBuilder setNumItineraries(int n) {
		routingRequest.setNumItineraries(n);
		return this;
	}

	/**
	 * Build a RoutingRequest from the accumulated parameters.
	 * 
	 * @return
	 */
	public RoutingRequest build() {
		// Set the graph at the end to avoid certain complications.
		if (graph != null) {
			routingRequest.setRoutingContext(graph);
		}
		
		return routingRequest;
	}
}