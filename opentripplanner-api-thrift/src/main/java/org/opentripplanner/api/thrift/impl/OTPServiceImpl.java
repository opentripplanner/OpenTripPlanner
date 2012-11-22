package org.opentripplanner.api.thrift.impl;

import java.util.List;

import lombok.Data;

import org.apache.thrift.TException;
import org.opentripplanner.api.thrift.OTPServerTask;
import org.opentripplanner.api.thrift.definition.NoPathFoundError;
import org.opentripplanner.api.thrift.definition.OTPService;
import org.opentripplanner.api.thrift.definition.TripDurationRequest;
import org.opentripplanner.api.thrift.definition.TripDurationResponse;
import org.opentripplanner.api.thrift.definition.TripParameters;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class OTPServiceImpl implements OTPService.Iface {

    private static Logger LOG = LoggerFactory.getLogger(OTPServerTask.class);
	
	private GraphService graphService;
	private PathService pathService;
	
	@Override
	public TripDurationResponse GetTripDuration(TripDurationRequest req)
			throws NoPathFoundError, TException {
		TripDurationResponse res = new TripDurationResponse();
		
		TripParameters trip = req.getTrip();
		RoutingRequest options = TripUtil.initRoutingRequest(trip);
		options.setRoutingContext(graphService.getGraph());
		List<GraphPath> paths = pathService.getPaths(options);
		
		// TODO(flamholz): do something reasonable when > 1 path found.
		if (paths.size() > 0) {
			GraphPath p = paths.get(0);
			res.setExpected_trip_duration(p.getDuration());
		} else {
			// TODO(flamholz): return some identifying information about the trip
			// inside the error.
			LOG.warn("Found no path for trip");
			throw new NoPathFoundError("No path found for your trip.");
		}
		
		return res;
	}
	
}