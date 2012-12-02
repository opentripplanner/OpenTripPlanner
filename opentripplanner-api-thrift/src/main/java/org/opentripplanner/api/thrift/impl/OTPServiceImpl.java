package org.opentripplanner.api.thrift.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.Data;

import org.apache.thrift.TException;
import org.opentripplanner.api.thrift.OTPServerTask;
import org.opentripplanner.api.thrift.definition.BulkPathsRequest;
import org.opentripplanner.api.thrift.definition.BulkPathsResponse;
import org.opentripplanner.api.thrift.definition.FindNearestEdgesRequest;
import org.opentripplanner.api.thrift.definition.FindNearestEdgesResponse;
import org.opentripplanner.api.thrift.definition.FindNearestVertexRequest;
import org.opentripplanner.api.thrift.definition.FindNearestVertexResponse;
import org.opentripplanner.api.thrift.definition.FindPathsRequest;
import org.opentripplanner.api.thrift.definition.FindPathsResponse;
import org.opentripplanner.api.thrift.definition.GraphVertex;
import org.opentripplanner.api.thrift.definition.GraphVerticesRequest;
import org.opentripplanner.api.thrift.definition.GraphVerticesResponse;
import org.opentripplanner.api.thrift.definition.OTPService;
import org.opentripplanner.api.thrift.definition.PathOptions;
import org.opentripplanner.api.thrift.definition.TripParameters;
import org.opentripplanner.api.thrift.definition.TripPaths;
import org.opentripplanner.api.thrift.util.EdgeMatchExtension;
import org.opentripplanner.api.thrift.util.GraphVertexExtension;
import org.opentripplanner.api.thrift.util.LatLngExtension;
import org.opentripplanner.api.thrift.util.RoutingRequestBuilder;
import org.opentripplanner.api.thrift.util.TripPathsExtension;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.CandidateEdge;
import org.opentripplanner.routing.impl.CandidateEdgeBundle;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

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
	 * Convenience getter for street index.
	 * 
	 * @return
	 */
	private StreetVertexIndexService getStreetIndex() {
		return graphService.getGraph().streetIndex;
	}

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
		LOG.info("GetVertices called");

		GraphVerticesResponse res = new GraphVerticesResponse();
		Graph g = graphService.getGraph();
		res.setVertices(makeGraphVertices(g));
		return res;
	}

	@Override
	public FindNearestVertexResponse FindNearestVertex(
			FindNearestVertexRequest req) throws TException {
		LOG.info("FindNearestVertex called");

		// NOTE(flamholz): can't set the graph here because we are not
		// actually doing any routing and don't have a to/from. From the
		// perspective of the street indes, RoutingRequest is really just
		// a container for the TraversalModes, which is a weird design
		// but it's what we've got to work with.
		RoutingRequestBuilder rrb = new RoutingRequestBuilder();
		if (req.isSetAllowed_modes()) {
			rrb.setTravelModes(req.getAllowed_modes());
		}
		RoutingRequest rr = rrb.build();

		// Get the nearest vertex
		StreetVertexIndexService streetVertexIndex = getStreetIndex();
		Coordinate c = new LatLngExtension(req.getLocation().getLat_lng())
				.toCoordinate();
		// NOTE(flamholz): We don't currently provide a name. 
		// I guess this would speed things up somewhat?
		Vertex closest = streetVertexIndex.getClosestVertex(c, null, rr);

		FindNearestVertexResponse res = new FindNearestVertexResponse();
		res.setNearest_vertex(new GraphVertexExtension(closest));
		return res;
	}
	
	@Override
	public FindNearestEdgesResponse FindNearestEdges(FindNearestEdgesRequest req)
			throws TException {
		LOG.info("FindNearestEdges called");

		// NOTE(flamholz): can't set the graph here because we are not
		// actually doing any routing and don't have a to/from. From the
		// perspective of the street index, RoutingRequest is really just
		// a container for the TraversalModes, which is a weird design
		// but it's what we've got to work with.
		RoutingRequestBuilder rrb = new RoutingRequestBuilder();
		if (req.isSetAllowed_modes()) {
			rrb.setTravelModes(req.getAllowed_modes());
		}
		RoutingRequest rr = rrb.build();

		// Get the nearest edges.
		StreetVertexIndexService streetVertexIndex = getStreetIndex();
		Coordinate c = new LatLngExtension(req.getLocation().getLat_lng())
				.toCoordinate();
		
		// Add matches to the response.
		FindNearestEdgesResponse res = new FindNearestEdgesResponse();
		CandidateEdgeBundle edges = streetVertexIndex.getClosestEdges(c, rr, null, null, false);
		int maxEdges = req.getMax_edges();
		for (CandidateEdge e : edges) {
			if (res.getNearest_edgesSize() >= maxEdges) break;
			res.addToNearest_edges(new EdgeMatchExtension(e));
		}
		
		return res;
	}

	/**
	 * Computes the GraphPath for the given trip.
	 * 
	 * @param trip
	 * @return
	 */
	private List<GraphPath> computePaths(TripParameters trip,
			PathOptions pathOptions) {
		// Build the RoutingRequest. For now, get only one itinerary.
		RoutingRequest options = (new RoutingRequestBuilder(trip))
				.setGraph(graphService.getGraph())
				.setNumItineraries(pathOptions.getNum_paths()).build();

		// For now, always use the default router.
		options.setRouterId("");

		return pathService.getPaths(options);
	}

	@Override
	public FindPathsResponse FindPaths(FindPathsRequest req) throws TException {
		LOG.info("FindPaths called");

		TripParameters trip = req.getTrip();
		TripPaths outPaths = new TripPaths();
		outPaths.setTrip(trip);

		List<GraphPath> computedPaths = computePaths(trip, req.getOptions());
		TripPathsExtension tripPaths = new TripPathsExtension(trip,
				computedPaths);

		FindPathsResponse res = new FindPathsResponse();
		res.setPaths(tripPaths);

		return res;
	}

	@Override
	public BulkPathsResponse BulkFindPaths(BulkPathsRequest req)
			throws TException {
		LOG.info("BulkFindPaths called");

		PathOptions pathOptions = req.getOptions();
		BulkPathsResponse res = new BulkPathsResponse();
		for (TripParameters trip : req.getTrips()) {
			List<GraphPath> computedPaths = computePaths(trip, pathOptions);
			TripPathsExtension tripPaths = new TripPathsExtension(trip,
					computedPaths);
			res.addToPaths(tripPaths);
		}
		return res;
	}

}