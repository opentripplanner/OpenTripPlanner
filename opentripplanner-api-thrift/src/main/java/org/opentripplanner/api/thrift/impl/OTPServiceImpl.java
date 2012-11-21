package org.opentripplanner.api.thrift.impl;

import java.util.List;

import lombok.Data;

import org.apache.thrift.TException;
import org.opentripplanner.api.thrift.definition.*;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.spt.GraphPath;

@Data
class OTPServiceImpl implements OTPService.Iface {

	private GraphService graphService;
	private PathService pathService;
	
	private String latLngToString(LatLng ll) {
		return String.format("%f,%f", ll.getLat(), ll.getLng());
	}
	
	private TraverseMode getTraverseMode(TravelMode m){
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
	
	private RoutingRequest buildRoutingRequest(TripParameters tripParams) {
		RoutingRequest options = new RoutingRequest();
		
		for (TravelMode m : tripParams.getAllowed_modes()) {
			TraverseMode tm = getTraverseMode(m);
			if (tm != null) options.addMode(tm);
		}
		
		Location origin = tripParams.getOrigin();
		Location destination = tripParams.getDestination();
		options.setFrom(latLngToString(origin.getLat_lng()));
		options.setTo(latLngToString(destination.getLat_lng()));
		
		Graph g = graphService.getGraph();
		options.setRoutingContext(g);
				
		return options;
	}
	
	@Override
	public TripDurationResponse GetTripDuration(TripDurationRequest req)
			throws TException {
		TripDurationResponse res = new TripDurationResponse();
		
		TripParameters trip = req.getTrip();
		RoutingRequest options = buildRoutingRequest(trip);
		List<GraphPath> paths = pathService.getPaths(options);
		
		// TODO(flamholz): do something reasonable when > 1 path found.
		if (paths.size() > 0) {
			GraphPath p = paths.get(0);
			res.setExpected_trip_duration(p.getDuration());
		} else {
			// TODO(flamholz): raise an error here.
			res.setExpected_trip_duration(0);	
		}
		
		return res;
	}
	
}