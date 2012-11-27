package org.opentripplanner.api.thrift.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.Data;

import org.apache.thrift.TException;
import org.opentripplanner.api.thrift.OTPServerTask;
import org.opentripplanner.api.thrift.definition.BulkTripDurationRequest;
import org.opentripplanner.api.thrift.definition.BulkTripDurationResponse;
import org.opentripplanner.api.thrift.definition.GraphVertex;
import org.opentripplanner.api.thrift.definition.GraphVerticesRequest;
import org.opentripplanner.api.thrift.definition.GraphVerticesResponse;
import org.opentripplanner.api.thrift.definition.NoPathFoundError;
import org.opentripplanner.api.thrift.definition.OTPService;
import org.opentripplanner.api.thrift.definition.TripDurationRequest;
import org.opentripplanner.api.thrift.definition.TripDurationResponse;
import org.opentripplanner.api.thrift.definition.TripParameters;
import org.opentripplanner.api.thrift.util.GraphVertexExtension;
import org.opentripplanner.api.thrift.util.RoutingRequestBuilder;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concrete implementation of the Thrift interface.
 *
 * @author flamholz
 */
@Data
public class OTPServiceImpl implements OTPService.Iface {

	private static Logger LOG = LoggerFactory.getLogger(OTPServerTask.class);

	private GraphService graphService;
	private PathService pathService;

	/**
	 * Returns all vertices in the graph as GraphVertices.
	 * 
	 * @param g
	 * @return
	 */
	private static List<GraphVertex> makeGraphVertices(Graph g) {
		Collection<Vertex> verts = g.getVertices();
		List<GraphVertex> l = new ArrayList<GraphVertex>(verts.size());
		for (Vertex v : verts) {
			l.add(new GraphVertexExtension(v));
		}
		return l;
	}
	
	@Override
	public GraphVerticesResponse GetVertices(GraphVerticesRequest req)
			throws TException {
		LOG.info("Received GetVerticesRequest");
		GraphVerticesResponse res = new GraphVerticesResponse();
		Graph g = graphService.getGraph();
		res.setVertices(makeGraphVertices(g));
		return res;
	}

	private int computePathDuration(TripParameters trip) throws NoPathFoundError {
		RoutingRequest options = (new RoutingRequestBuilder(trip))
				.setGraph(graphService.getGraph())
				.setNumItineraries(1)	// For now, only get 1 itinerary.
				.build();
				
		// For now, always use the default router.
		options.setRouterId("");
			
		List<GraphPath> paths = pathService.getPaths(options);
		
		// TODO(flamholz): do something reasonable when > 1 path found.
		if (paths == null || paths.size() == 0) {
			// TODO(flamholz): return some identifying information about the trip
			// inside the error.
			LOG.warn("Found no path for trip");
			throw new NoPathFoundError("No path found for your trip.");
		}
		else {
			GraphPath p = paths.get(0);
			return p.getDuration();			
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
				expectedTimes.add(-1); // Sentinel.
			}
		}

		res.setExpected_trip_durations(expectedTimes);
		return res;
	}

}