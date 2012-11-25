package org.opentripplanner.api.thrift.impl;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import org.apache.thrift.TException;
import org.opentripplanner.api.thrift.OTPServerTask;
import org.opentripplanner.api.thrift.definition.BulkTripDurationRequest;
import org.opentripplanner.api.thrift.definition.BulkTripDurationResponse;
import org.opentripplanner.api.thrift.definition.GraphVerticesRequest;
import org.opentripplanner.api.thrift.definition.GraphVerticesResponse;
import org.opentripplanner.api.thrift.definition.NoPathFoundError;
import org.opentripplanner.api.thrift.definition.OTPService;
import org.opentripplanner.api.thrift.definition.TripDurationRequest;
import org.opentripplanner.api.thrift.definition.TripDurationResponse;
import org.opentripplanner.api.thrift.definition.TripParameters;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
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
	public GraphVerticesResponse GetVertices(GraphVerticesRequest req)
			throws TException {
		LOG.info("Received GetVerticesRequest");
		GraphVerticesResponse res = new GraphVerticesResponse();
		Graph g = graphService.getGraph();
		if (g == null) {
			LOG.warn("Graph is null");
		}
		res.setVertices(GraphUtil.getGraphVertices(graphService.getGraph()));
		return res;
	}

	private int computePathDuration(TripParameters trip) throws NoPathFoundError {
		RoutingRequest options = TripUtil.initRoutingRequest(trip);
		// For now, only get 1 itinerary.
		options.setNumItineraries(1);	
		options.setRoutingContext(graphService.getGraph());
			
		List<GraphPath> paths = pathService.getPaths(options);
		
		// TODO(flamholz): do something reasonable when > 1 path found.
		if (paths.size() > 0) {
			GraphPath p = paths.get(0);
			return p.getDuration();
		} else {
			// TODO(flamholz): return some identifying information about the trip
			// inside the error.
			LOG.warn("Found no path for trip");
			throw new NoPathFoundError("No path found for your trip.");
		}
	}
	
	@Override
	public TripDurationResponse GetTripDuration(TripDurationRequest req)
			throws NoPathFoundError, TException {
		TripDurationResponse res = new TripDurationResponse();
		res.setExpected_trip_duration(computePathDuration(req.getTrip()));
		return res;
	}

	@Override
	public BulkTripDurationResponse GetManyTripDurations(
			BulkTripDurationRequest req) throws TException {
		BulkTripDurationResponse res = new BulkTripDurationResponse();
		List<Integer> expectedTimes = new ArrayList<Integer>(req.getTripsSize());
		
		for (TripParameters trip : req.getTrips()) {
			try {
				expectedTimes.add(computePathDuration(trip));
			} catch (NoPathFoundError e) {
				expectedTimes.add(-1);  // Sentinel.
			}
		}
		
		res.setExpected_trip_durations(expectedTimes);
		return res;
	}
	
}